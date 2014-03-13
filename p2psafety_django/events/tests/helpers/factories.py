from factory import SubFactory
from factory.django import DjangoModelFactory, FileField

from events.models import Event, EventUpdate
from users.tests.helpers import UserFactory


class EventFactory(DjangoModelFactory):
    FACTORY_FOR = Event

    user = SubFactory(UserFactory)


class EventUpdateFactory(DjangoModelFactory):
    FACTORY_FOR = EventUpdate
