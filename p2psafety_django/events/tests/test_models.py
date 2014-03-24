import mock
import time

from datetime import timedelta
from django.contrib.gis.geos import Point
from django.conf import settings
from django.test import TestCase
from django.test.utils import override_settings

from core.utils import set_livesettings_value
from .helpers.factories import EventFactory, EventUpdateFactory
from ..jabber.tests.helpers import MockedEventsNotifierClient
from ..models import Event, EventUpdate
from users.tests.helpers import UserFactory
from .helpers.mixins import CeleryMixin
from ..tasks import eventupdate_watchdog


class EventTestCase(CeleryMixin, TestCase):

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

    def test_auto_activate_with_watchdog(self):
        """Test watchdog mode
        create passive event (passive mode)
        start watchdog(celery task in real life),
        it will check for updates with passive=true to come
        while updates come -> even stays passive
        if no updates in timeout -> activate alarm
        """

        event = EventFactory()
        EventUpdateFactory(event=event,active=False)

        self.assert_task_sent(eventupdate_watchdog,
                event.id,
                timedelta(seconds=settings.WATCHDOG_DELAY))
        self.assertEquals(len(self.applied_tasks),1)
        self.assertTrue(event.watchdog_task_id)
        #new passive update
        EventUpdateFactory(event=event,active=False)
        #but only one watchdog
        self.assertEquals(len(self.applied_tasks),1)
        event = Event.objects.get(id=event.id)
        self.assertEquals(event.status,Event.STATUS_PASSIVE)
        #so we run watchdog now
        #no events,
        time.sleep(1)
        eventupdate_watchdog(event.id,timedelta(seconds=1))
        eu = EventUpdate.objects.latest('id')
        event = Event.objects.get(id=event.id)
        self.assertEquals(event.status,Event.STATUS_ACTIVE)
        


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

