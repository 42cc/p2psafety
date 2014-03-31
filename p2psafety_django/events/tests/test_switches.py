import mock

from django.test import TestCase

from core.utils import set_livesettings_value
from .helpers.factories import EventFactory, EventUpdateFactory


class EventSwitchesTestCase(TestCase):

    @mock.patch('events.models.jabber')
    def test_supporters_autonotify(self, mock_jabber):
        event1, event2 = EventFactory(), EventFactory()

        set_livesettings_value('Events', 'supporters-autonotify', False)
        EventUpdateFactory(event=event1)
        self.assertFalse(mock_jabber.notify_supporters.called)

        set_livesettings_value('Events', 'supporters-autonotify', True)
        EventUpdateFactory(event=event2)
        self.assertEqual(mock_jabber.notify_supporters.call_count, 1)
