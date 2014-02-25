import functools
import logging
from contextlib import contextmanager

from django.conf import settings
from django.core.exceptions import ImproperlyConfigured

from sleekxmpp import ClientXMPP
from sleekxmpp.xmlstream import ET
from lxml import etree


logger = logging.getLogger('events.jabber')


class PubsubClient(object):

    class Config(object):
        __slots__ = ('jid', 'password', 'pubsub_server', 'node_name')

    @classmethod
    def _parse_config_dict(cls, config_dict):
        config = cls.Config()
        try:
            config.jid = config_dict['JID']
            config.password = config_dict['PASSWORD']
            config.pubsub_server = config_dict['PUBSUB_SERVER']
            config.node_name = config_dict['NODE_NAME']
        except KeyError, e:
            message = 'Key %s not found within client config' % e.args[1]
            raise ImproperlyConfigured(message)
        else:
            return config

    def __init__(self, config_dict):
        self.config = self._parse_config_dict(config_dict)
        self._client = ClientXMPP(self.config.jid, self.config.password)
        # Service discovery
        self._client.register_plugin('xep_0030')
        # Result Set Management
        self._client.register_plugin('xep_0059')
        # Publish-subscribe
        self._client.register_plugin('xep_0060')
        self._callback = lambda: None

    @property
    def _pubsub(self):
        return self._client['xep_0060']

    def __enter__(self):
        logger.debug('connecting as %s', self.config.jid)
        if self._client.connect():
            logger.debug('connected succesfully')
            self._client.process(block=False)
        else:
            logger.error('failed to connect')
        return self

    def __exit__(self, exc_type, exc_value, traceback):
        self._client.disconnect()
        logger.debug('disconnected')
        if exc_value:
            raise exc_value

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


def notify_supporters(event):
    """
    Sends notifications to event's supporters via jabber node.

    :type event: :class:`events.models.Event`
    """
    from events.api.resources import EventResource

    # Constructing payload
    resource = EventResource()
    event_dict = resource.full_dehydrate(resource.build_bundle(obj=event))
    payload = resource.serialize(None, event_dict, 'application/xml')

    with PubsubClient(settings.EVENTS_NOTIFIER) as client:
        client.publish(payload)


def notify_supporter(event, supporter):
    raise NotImplementedError
