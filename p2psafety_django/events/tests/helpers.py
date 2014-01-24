from factory import SubFactory, Sequence
from factory.django import DjangoModelFactory

from django.contrib.auth.models import User
from django.contrib.gis.geos import Point

from ..models import Event, EventUpdate


class UserFactory(DjangoModelFactory):
    FACTORY_FOR = User

    username = Sequence(lambda n: 'user%d' % n)


class EventFactory(DjangoModelFactory):
    FACTORY_FOR = Event

    user = SubFactory(UserFactory)


class EventUpdateFactory(DjangoModelFactory):
    FACTORY_FOR = EventUpdate
