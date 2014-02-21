from contextlib import contextmanager

from django.conf import settings

from sleekxmpp import ClientXMPP
from sleekxmpp.xmlstream import ET


class MyClient(object):

    def __init__(self, jid, password):
        self.client = ClientXMPP(jid, password)
        self.client.register_plugin('xep_0030')
        self.client.register_plugin('xep_0059')
        self.client.register_plugin('xep_0060')

    @contextmanager
    def connect(self):
        self.client.connect()
        self.client.process(block=False)
        try:
            yield
        except Exception, e:
            print e
        finally:
            self.client.disconnect()

    def publish(self, pubsub_server, node, payload):
        self.client['xep_0060'].publish(pubsub_server, node, payload=payload)


class EventsNotifier(object):

    class Config(dict):
        __getattr__ = dict.__getitem__
        __setattr__ = dict.__setitem__

    def __init__(self):
        self.cfg = self.__get_config()
        self.client = MyClient(self.cfg.jid, self.cfg.password)

    def __get_config(self):
        cfg = EventsNotifier.Config()
        notifier_settings = getattr(settings, 'EVENTS_NOTIFIER', {})
        cfg.jid = notifier_settings.get('JID')
        cfg.password = notifier_settings.get('PASSWORD')
        cfg.node = notifier_settings.get('NODE')
        cfg.pubsub_server = notifier_settings.get('PUBSUB_SERVER')

        if all(cfg.values()):
            return cfg
        else:
            raise NotImplementedError("Specify event's notifier settings")

    def notify_supporters(self, event):
        """
        :type event: :class:`events.models.Event`
        """
        payload = self._construct_payload(event)
        with self.client.connect():
            self.client.publish(self.cfg.pubsub_server, self.cfg.node, payload=payload)

    def notify_supporter(self, event, supporter_user):
        raise NotImplementedError

    def get_jid(self, user):
        return user.username

    def _construct_payload(self, event):
        from events.api.resources import EventResource
        resource = EventResource()
        bundle = resource.build_bundle(obj=event)
        dehydrated = resource.full_dehydrate(bundle)
        xml = resource.serialize(None, dehydrated, 'application/xml')
        payload = ET.fromstring(xml)
        return payload
