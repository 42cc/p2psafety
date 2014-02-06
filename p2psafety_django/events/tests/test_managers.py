from django.test import TestCase


from .helpers.factories import EventFactory
from ..models import Event
from users.tests.helpers import UserFactory


class EventsManagerTestCase(TestCase):

    def test_get_current_active_of(self):
        user_with_active_event = UserFactory()
        user_without_active_event = UserFactory()
        passive_event = EventFactory(user=user_without_active_event)
        active_event = EventFactory(user=user_with_active_event,
                                    status=Event.STATUS_ACTIVE)

        actual_active_event = Event.objects.get_current_active_of(user_with_active_event)
        self.assertEquals(actual_active_event, active_event)

        with self.assertRaises(Event.DoesNotExist):
            Event.objects.get_current_active_of(user_without_active_event)
