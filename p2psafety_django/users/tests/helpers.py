from django.contrib.auth.models import User

from factory import Sequence
from factory.django import DjangoModelFactory


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
