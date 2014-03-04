import mock

from core.tests import SwithcesTestCase
from .helpers.factories import EventFactory, EventUpdateFactory


class EventSwitchesTestCase(SwithcesTestCase):

    @mock.patch('events.models.jabber')
    def test_supporters_autonotify(self, mock_jabber):
        event1, event2 = EventFactory(), EventFactory()

        self.set_switch('supporters-autonotify', False)
        EventUpdateFactory(event=event1)
        self.assertFalse(mock_jabber.notify_supporters.called)

        self.set_switch('supporters-autonotify')
        EventUpdateFactory(event=event2)
        self.assertEqual(mock_jabber.notify_supporters.call_count, 1)
