from mock import patch
from factory import SubFactory, Sequence
from factory.django import DjangoModelFactory, FileField

from django.contrib.auth.models import User

from ..models import Event, EventUpdate


class UserFactory(DjangoModelFactory):
    FACTORY_FOR = User
    FACTORY_DJANGO_GET_OR_CREATE = ('username',)

    username = Sequence(lambda n: 'user%d' % n)


class EventFactory(DjangoModelFactory):
    FACTORY_FOR = Event

    user = SubFactory(UserFactory)


class EventUpdateFactory(DjangoModelFactory):
    FACTORY_FOR = EventUpdate

    audio = FileField()
    video = FileField()


def mock_get_backend(module_path='events.api.resources'):
    def decorator(func):
        def decorated(self, *args, **kwargs):
            with patch(module_path + '.get_backend') as mocked_get_backend:
                self.auth_user = UserFactory()
                self.mocked_get_backend = mocked_get_backend
                mocked_get_backend()().do_auth.return_value = self.auth_user
                result = func(self, *args, **kwargs)
                del self.auth_user
            return result
        return decorated
    return decorator
