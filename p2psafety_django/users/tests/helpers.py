from django.core.urlresolvers import reverse
from django.contrib.auth.models import User

from factory import Sequence
from factory.django import DjangoModelFactory

from ..models import Role


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


class RoleFactory(DjangoModelFactory):
    FACTORY_FOR = Role

    name = Sequence(lambda n: 'role%d' % n)


class ModelsMixin(object):
    @property
    def roles_list_url(self):
        return reverse('api_dispatch_list', kwargs=dict(resource_name='roles',
                                                        api_name='v1'))

    def roles_detail_url(self, role_id):
        kwargs = dict(resource_name='roles', api_name='v1', pk=role_id)
        return reverse('api_dispatch_detail', kwargs=kwargs)

    @property
    def users_list_url(self):
        kwargs = dict(resource_name='users', api_name='v1')
        return reverse('api_dispatch_list', kwargs=kwargs)

    def users_detail_url(self, user_id):
        kwargs = dict(resource_name='users', api_name='v1', pk=user_id)
        return reverse('api_dispatch_detail', kwargs=kwargs)

    def users_role_url(self, user_id, role_id):
        kwargs = dict(resource_name='users', api_name='v1', pk=user_id, role_pk=role_id)
        return reverse('api_users_role', kwargs=kwargs)
