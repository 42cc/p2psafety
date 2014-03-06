import mock

from django.test import TestCase

from livesettings import ConfigurationSettings, config_value

from .helpers.factories import EventFactory, EventUpdateFactory


class EventSwitchesTestCase(TestCase):

    def set_value(self, group_name, value_name, value_value):
        mgr = ConfigurationSettings()
        config_field = mgr.get_config(group_name, value_name)
        config_field.update(value_value)

    @mock.patch('events.models.jabber')
    def test_supporters_autonotify(self, mock_jabber):
        event1, event2 = EventFactory(), EventFactory()

        # self.set_switch('supporters-autonotify', False)
        self.set_value('Events', 'supporters-autonotify', False)
        EventUpdateFactory(event=event1)
        self.assertFalse(mock_jabber.notify_supporters.called)

        # self.set_switch('supporters-autonotify')
        self.set_value('Events', 'supporters-autonotify', True)
        EventUpdateFactory(event=event2)
        self.assertEqual(mock_jabber.notify_supporters.call_count, 1)
