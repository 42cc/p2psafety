import factory

from django.contrib.auth.models import User

from ..models import Event


class UserFactory(factory.django.DjangoModelFactory):
    FACTORY_FOR = User


class EventFactory(factory.django.DjangoModelFactory):
    FACTORY_FOR = Event

    user = factory.SubFactory(UserFactory)