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

from . import logger
from users.utils import get_api_key


class BaseConfig(object):

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
                map(self.create_jabber_account, users_to_create)
            else:
                logger.debug('no need to create additional accounts')
        logger.info('synchronize completed')

    def create_jabber_account(self, user):
        """
        :type user: `django.contrib.auth.models.User`
        """
        on_done_event = threading.Event()
        jabber_username = user.username + '@p2psafety.net'
        jabber_password = get_api_key(user).key
        logger.debug('creating account for "%s" with jid=%s passsword=%s',
                     user.username, jabber_username, jabber_password)

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
            on_done_event.set()

        def command_error(iq, session):
            code, text = iq['error']['code'], iq['error']['text']
            logger.error('could not create account: %s %s', code, text)
            self._adhoc.terminate_command(session)
            on_done_event.set()

        session = dict(next=process_form, error=command_error)
        self._admin.add_user(session=session)
        on_done_event.wait()

class PubsubClient(BaseClient):

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
    def _pubsub(self):
        return self._client['xep_0060']

    def publish(self, payload):
        if isinstance(payload, basestring):
            payload = ET.fromstring(payload)

        if logger.level is logging.DEBUG:
            lxml_payload = etree.fromstring(ET.tostring(payload))
            str_payload = etree.tostring(lxml_payload, pretty_print=True)
            logger.debug('sending publish message with payload:\n%s', str_payload)

        self._pubsub.publish(self.config.pubsub_server,
                             self.config.node_name,
                             payload=payload)


def get_client(ClientClassOrName):
    if isinstance(ClientClassOrName, basestring):
        ClientClass = globals()[ClientClassOrName]
    elif isinstance(ClientClassOrName, type):
        ClientClass = ClientClassOrName
    else:
        raise TypeError(ClientClassOrName)

    if ClientClass is PubsubClient:
        config_dict = settings.EVENTS_NOTIFIER
    elif ClientClass is UsersClient:
        config_dict = settings.JABBER_ADMIN_CLIENT
    else:
        raise Exception('No such client')

    return ClientClass(config_dict)
