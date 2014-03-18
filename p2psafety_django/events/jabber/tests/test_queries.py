from mock import patch

from django.test import TestCase
from django.test.utils import override_settings

from events.tests.helpers.factories import EventFactory
from .. import queries, clients


@override_settings(JABBER_DRY_RUN=True)
class QueriesTestCase(TestCase):

    @override_settings(XMPP_EVENTS_NOTIFICATION_RADIUS=123)
    @patch.object(clients, 'EventsNotifierClient')
    def test_notify_supporters_default_radius(self, MockClientClass):
        event = EventFactory()

        with override_settings(JABBER_DRY_RUN=False):
            queries.notify_supporters(event)

        mock_client = MockClientClass.return_value.__enter__.return_value
        mock_client.publish.assert_called_once_with(event, 123)

    @patch.object(clients, 'EventsNotifierClient')
    def test_notify_supporters_manual_radius(self, MockClientClass):
        event = EventFactory()

        with override_settings(JABBER_DRY_RUN=False):
            queries.notify_supporters(event, 0)

        mock_client = MockClientClass.return_value.__enter__.return_value
        mock_client.publish.assert_called_once_with(event, 0)
