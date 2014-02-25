import mock

from core.tests import SwithcesTestCase
from .. import jabber
from .helpers.factories import EventFactory, EventUpdateFactory


class EventSwitchesTestCase(SwithcesTestCase):

    def test_supporters_autonotify(self):
        event = EventFactory()
        event.notify_supporters = mock_notify_supporters = mock.MagicMock()
        event_supporter = EventFactory()
        event.supporters.add(event_supporter)

        self.set_switch('supporters-autonotify', False)
        EventUpdateFactory(event=event)
        self.assertFalse(mock_notify_supporters.called)

        self.set_switch('supporters-autonotify')
        EventUpdateFactory(event=event)
        self.assertEqual(mock_notify_supporters.call_count, 1)
