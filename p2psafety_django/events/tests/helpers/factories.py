from factory import SubFactory, Sequence
from factory.django import DjangoModelFactory, FileField

from django.contrib.auth.models import User

from events.models import Event, EventUpdate


class UserFactory(DjangoModelFactory):
    FACTORY_FOR = User
    FACTORY_DJANGO_GET_OR_CREATE = ('username',)

    username = Sequence(lambda n: 'user%d' % n)
    password = Sequence(lambda n: 'user%d' % n)

    @classmethod
    def _create(cls, target_class, *args, **kwargs):
        manager = cls._get_manager(target_class)
        user = manager.create_user(*args, **kwargs)
        user.real_password = kwargs.get('password')
        return user


class EventFactory(DjangoModelFactory):
    FACTORY_FOR = Event

    user = SubFactory(UserFactory)


class EventUpdateFactory(DjangoModelFactory):
    FACTORY_FOR = EventUpdate

    audio = FileField()
    video = FileField()
