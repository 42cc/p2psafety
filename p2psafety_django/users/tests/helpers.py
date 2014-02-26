from django.core.urlresolvers import reverse
from django.contrib.auth.models import User
from django.contrib.sites.models import Site
from django.test.utils import override_settings

from allauth.account import app_settings as account_settings
from allauth.socialaccount.models import SocialApp
from allauth.socialaccount.providers.facebook.provider import FacebookProvider
from allauth.socialaccount.providers.google.provider import GoogleProvider
from factory import Sequence
from factory.django import DjangoModelFactory
from tastypie.test import ResourceTestCase

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


class MovementTypeFactory(DjangoModelFactory):
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

    def users_roles_url(self, user_id):
        kwargs = dict(resource_name='users', api_name='v1', pk=user_id)
        return reverse('api_users_roles', kwargs=kwargs)


@override_settings(
    ACCOUNT_EMAIL_VERIFICATION=account_settings.EmailVerificationMethod.NONE,
    SOCIALACCOUNT_AUTO_SIGNUP=False,
    SOCIALACCOUNT_PROVIDERS=dict(
        facebook=dict(VERIFIED_EMAIL=False),
        google=dict(),
    ))
class SocialTestCase(ResourceTestCase):

    def setUp(self):
        super(SocialTestCase, self).setUp()
        current_site = Site.objects.get_current()
        for provider in (FacebookProvider, GoogleProvider):
            kwargs = dict(provider=provider.id, name=provider.id,
                          client_id='app123id', key=provider.id, secret='dummy')
            SocialApp.objects.create(**kwargs).sites.add(current_site)

