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
