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
            plugin_name = 'xep_' + str(plugin_num).rjust(4, '0')
            self._client.register_plugin(plugin_name)

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

    @property
    def discovery(self):
        return self._client['xep_0030']

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

    required_plugins = tuple()

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
            else:
                logger.info('no need to create additional accounts')
                

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
