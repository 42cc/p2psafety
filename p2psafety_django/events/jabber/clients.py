import functools
import logging
import traceback
import threading
from contextlib import contextmanager

from django.conf import settings
from django.contrib.auth.models import User
from django.core.exceptions import ImproperlyConfigured

from sleekxmpp import ClientXMPP
from sleekxmpp.exceptions import IqError, IqTimeout
from sleekxmpp.jid import JID
from sleekxmpp.xmlstream import ET
from lxml import etree

from users.utils import get_api_key


logger = logging.getLogger('events.jabber')


class BaseConfig(object):
    """
    Base class for jabber clients configurations.
    Extend and add new fields to __slots__ class variable, so the only these
    fields can be set on an config object.
    """
    __slots__ = ('jid', 'password')

    @contextmanager
    def _handle_missing_keys(self):
        try:
            yield
        except KeyError, e:
            message = 'Key %s not found within client config' % e.args[1]
            raise ImproperlyConfigured(message)

    def __init__(self, config_dict):
        with self._handle_missing_keys():
            self.jid = config_dict['JID']
            self.password = config_dict['PASSWORD']


class BaseClient(object):
    """
    Lightweight wrapper for :class:`sleekxmpp.ClientXMPP` client.
        
    Example of usage::

        with BaseClient({...}) as client:            
            client.my_method(...)
    or::

        client = BaseClient({...})
        client.connect()
        client.my_method(...)
        client.disconnect()

    """
    base_required_plugins = 30, # Service discovery
    Config = BaseConfig

    def __init__(self, config_dict):
        self.config = self.Config(config_dict)
        self._client = ClientXMPP(self.config.jid, self.config.password)

        required_plugins = set(self.base_required_plugins + self.required_plugins)
        for plugin_num in required_plugins:
            self._client.register_plugin(self._get_plugin_name(plugin_num))

        self._client.add_event_handler('session_start', self._on_start, threaded=True)
        self._on_start_event = threading.Event()

    def __enter__(self):
        self.connect()
        return self

    def __exit__(self, exc_type, exc_value, tb):
        self.disconnect()
        if exc_value:
            traceback.print_exc()
            raise exc_value

    def _on_start(self, event):
        logger.debug('session has been started')
        self._on_start_event.set()

    def _get_plugin_name(self, plugin_num):
        return 'xep_' + str(plugin_num).rjust(4, '0')

    def get_plugin(self, plugin_num):
        return self._client[self._get_plugin_name(plugin_num)]

    @property
    def discovery(self): return self.get_plugin(30)

    @property
    def is_connected(self):
        return self._client.authenticated

    def connect(self):
        logger.debug('connecting as %s', self.config.jid)
        if self._client.connect():
            self._client.process(block=False)
            # Make sure session was started
            self._on_start_event.wait()
        else:
            logger.error('failed to connect')

    def disconnect(self):
        self._client.disconnect()
        logger.debug('disconnected')


class UsersClient(BaseClient):

    required_plugins = 133, # Administration service

    @property
    def _admin(self): return self.get_plugin(133)

    @property
    def _adhoc(self): return self.get_plugin(50)

    def synchronize_accounts(self):
        """
        Creates jabber accounts for registered users.
        """
        node, jid = 'all users', self._client.boundjid.server
        
        try:
            logger.debug('sending "get items" to %s node %s', jid, node)
            items = self.discovery.get_items(jid=jid, node=node, block=True)
        except IqError as e:
            logging.error("Entity returned an error: %s" % e.iq['error']['condition'])
        except IqTimeout:
            logging.error("No response received.")
        else:
            registered_users = User.objects.only('id', 'username').order_by('id')
            registered_jids = [item[0] for item in items['disco_items']['items']]
            name_user_map = dict((u.username, u) for u in registered_users)
            users_to_create = [name_user_map[name] for name in name_user_map
                               if name not in registered_jids]
            if users_to_create:
                logger.debug('%d jabber profiles are missing', len(users_to_create))
                created_count = sum(map(self.create_account, users_to_create))
                logger.info('created %d accounts', created_count)
            else:
                logger.debug('no need to create additional accounts')
        logger.info('synchronize completed')

    def create_account(self, user):
        """
        Creates jabber account for given user.
        Uses `user.username` as jid and user's api pkey as password.
        Returns True if account was created successfully and False otherwise.

        :type user: `django.contrib.auth.models.User`
        :rtype: True or False
        """        
        jabber_username = user.username + '@p2psafety.net'
        jabber_password = get_api_key(user).key
        logger.debug('creating account for "%s" with jid=%s passsword=%s',
                     user.username, jabber_username, jabber_password)

        # Both current and process loop threads have access to this variable
        shared_result = dict(result=False)
        # We use event to be able to return result within current thread
        on_done_event = threading.Event()

        def process_form(iq, session):
            form = iq['command']['form']
            answers = {
                'accountjid': jabber_username,
                'password': jabber_password,
                'password-verify': jabber_password,
                'FORM_TYPE': form['fields']['FORM_TYPE']['value']
            }
            form['type'] = 'submit'
            form['values'] = answers

            session['next'] = command_success
            session['payload'] = form

            self._adhoc.complete_command(session)

        def command_success(iq, session):            
            logger.debug('success')
            shared_result['result'] = True
            on_done_event.set()

        def command_error(iq, session):
            code, text = iq['error']['code'], iq['error']['text']
            logger.error('could not create account: %s %s', code, text)
            self._adhoc.terminate_command(session)
            on_done_event.set()

        session = dict(next=process_form, error=command_error)
        self._admin.add_user(session=session)
        on_done_event.wait()
        return shared_result['result']

class EventsNotifierClient(BaseClient):

    required_plugins = (59, # Result Set Management
                        60) # Publish-subscribe

    class Config(BaseConfig):
        __slots__ = ('pubsub_server', 'node_name')

        def __init__(self, config_dict):
            super(type(self), self).__init__(config_dict)
            with self._handle_missing_keys():
                self.pubsub_server = config_dict['PUBSUB_SERVER']
                self.node_name = config_dict['NODE_NAME']

    @property
    def _pubsub(self): return self.get_plugin(60)

    def publish(self, event, radius):
        from events.api.resources.jabber import EventResource

        resource = EventResource()
        event_dict = resource.full_dehydrate(resource.build_bundle(obj=event))
        event_dict.data['radius'] = radius
        str_payload = resource.serialize(None, event_dict, 'application/xml')
        payload = ET.fromstring(str_payload)

        if logger.level is logging.DEBUG:
            lxml_payload = etree.fromstring(ET.tostring(payload))
            str_payload = etree.tostring(lxml_payload, pretty_print=True)
            logger.debug('sending publish message with payload:\n%s', str_payload)

        self._pubsub.publish(self.config.pubsub_server,
                             self.config.node_name,
                             payload=payload)


def get_client(ClientClassOrName):
    """
    Constructs client object using proper settings by given class object or name.

    :type ClientClassOrName: type or basestring
    :rtype: BaseClient
    """
    if isinstance(ClientClassOrName, basestring):
        ClientClass = globals()[ClientClassOrName]
    elif isinstance(ClientClassOrName, type):
        ClientClass = ClientClassOrName
    else:
        raise TypeError(ClientClassOrName)

    if ClientClass is EventsNotifierClient:
        config_dict = settings.EVENTS_NOTIFIER_CLIENT
    elif ClientClass is UsersClient:
        config_dict = settings.USERS_CLIENT
    else:
        raise Exception('No such client')

    return ClientClass(config_dict)
