from django.test import TestCase


from .helpers.factories import EventFactory
from ..models import Event
from users.tests.helpers import UserFactory


class EventsManagerTestCase(TestCase):

    def test_get_current_of(self):
        user_with_active_event = UserFactory()
        user_with_passive_event = UserFactory()
        user_with_finished_event = UserFactory()
        active_event = EventFactory(user=user_with_active_event,
                                    status=Event.STATUS_ACTIVE)
        passive_event = EventFactory(user=user_with_passive_event,
                                     status=Event.STATUS_PASSIVE)
        finished_event = EventFactory(user=user_with_finished_event,
                                      status=Event.STATUS_FINISHED)

        self.assertEquals(active_event, Event.objects.get_current_of(user_with_active_event))
        self.assertEquals(passive_event, Event.objects.get_current_of(user_with_passive_event))
        with self.assertRaises(Event.DoesNotExist):
            Event.objects.get_current_of(user_with_finished_event)
