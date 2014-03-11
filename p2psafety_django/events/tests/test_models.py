import mock

from django.contrib.gis.geos import Point
from django.test import TestCase
from django.test.utils import override_settings

from core.utils import set_livesettings_value
from .helpers.factories import EventFactory, EventUpdateFactory
from ..jabber import clients
from ..jabber.tests.helpers import MockedEventsNotifierClient
from ..models import Event
from users.tests.helpers import UserFactory


class EventTestCase(TestCase):

    def test_support_by_user(self):
        user_victim, user_supporter = UserFactory(), UserFactory()
        event_victim = EventFactory(user=user_victim)
        event_supporter = EventFactory(user=user_supporter,
                                       status=Event.STATUS_ACTIVE)

        event_victim.support_by_user(user_supporter)

        event_supporter = Event.objects.get(id=event_supporter.id)
        self.assertEquals(event_supporter.type, Event.TYPE_SUPPORT)
        self.assertEquals(list(event_victim.supporters.order_by('id')),
                          [event_supporter])
        self.assertEquals(list(event_supporter.supported.order_by('id')),
                          [event_victim])

    def test_latest_text(self):
        event = EventFactory()
        EventUpdateFactory(event=event)
        EventUpdateFactory(event=event, text='123')
        EventUpdateFactory(event=event, text='1234')
        EventUpdateFactory(event=event)

        self.assertEquals(event.latest_text, '1234')

    def test_latest_location(self):
        event = EventFactory()
        EventUpdateFactory(event=event)
        EventUpdateFactory(event=event, location=Point(1, 2))
        EventUpdateFactory(event=event, location=Point(3, 4))
        EventUpdateFactory(event=event)        

        self.assertEquals(event.latest_location, Point(3, 4))


class EventUpdateTestCase(TestCase):

    @mock.patch('events.jabber.clients.EventsNotifierClient')
    def test_save(self, MockClient):
        set_livesettings_value('Events', 'supporters-autonotify', True)        
        mocked_client = MockClient.return_value = MockedEventsNotifierClient()
        event = EventFactory()

        with override_settings(JABBER_DRY_RUN=False):
            EventUpdateFactory(event=event, text='Test', location=Point(1, 2))
        
        mocked_client.assert_published_once()
        self.assertIn('Test', mocked_client.payload_string)
        self.assertIn('location type="hash"', mocked_client.payload_string)
