from django.contrib.gis.geos import Point
from django.test import TestCase

from .helpers.factories import EventFactory, EventUpdateFactory
from ..models import Event

from users.tests.helpers import UserFactory


class EventsTestCase(TestCase):

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
