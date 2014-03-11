import mock
from lxml import etree
from sleekxmpp.xmlstream import ET

from .. import clients


def assertXMLEqual(a, b):
    """
    Asserts that both xmls should be syntactically valid and equal.
    Prints human-readable xml on `AssertionError`.
    """
    args = [a, b]
    parser = etree.XMLParser(remove_blank_text=True)
    for i, _ in enumerate(args):
        if isinstance(args[i], basestring):
            args[i] = etree.XML(args[i], parser=parser)
        elif isinstance(args[i], etree._Element):
            pass
        else:
            args[i] = etree.fromstring(ET.tostring(args[i]))

        args[i] = etree.tostring(args[i], pretty_print=True)

    assert_message = ('XMLs are not equal:\n'
                      '{2}\n{0}{2}\n{1}'
                      .format(args[0], args[1],  '-' * 70))
    assert (args[0] == args[1]), assert_message


class MockedEventsNotifierClient(clients.EventsNotifierClient):
    """
    Mock-like object, gives ability to track sended payloads.
    """
    def __init__(self, *args, **kwargs):
        object.__init__(self)
        cfg = dict(JID='a@a.c', PASSWORD='a', PUBSUB_SERVER='a', NODE_NAME='')
        self.config = clients.EventsNotifierClient.Config(cfg)
        self._pubsub_plugin = mock.MagicMock()

    def get_plugin(self, num):
        return self._pubsub_plugin    

    @property
    def payload(self):
        return self._pubsub.publish.call_args[1].get('payload')

    @property
    def payload_list(self):
        return [arg[1].get('payload') for arg in self._pubsub.publish.call_args_list]

    @property
    def publish_count(self):
        return self._pubsub.publish.call_count

    def reset_mock(self):
        self._pubsub.publish.reset_mock()

    def assert_published_once_with(self, expected_payload):
        assert (self.publish_count == 1)
        assertXMLEqual(self.payload, expected_payload)


