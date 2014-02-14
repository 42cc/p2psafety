from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from .factories import UserFactory


class ModelsMixin(object):
    def __url(self, url_name, *args, **kwargs):
        kwargs['api_name'] = 'v1'
        if 'res_name' in kwargs:
            kwargs['resource_name'] = kwargs.pop('res_name')
        return reverse(url_name, args=args, kwargs=kwargs)

    def events_support_url(self, event_id):
        return self.__url('api_events_support', res_name='events', pk=event_id)

    @property
    def events_list_url(self):
        return self.__url('api_dispatch_list', res_name='events')

    @property
    def eventupdates_list_url(self):
        return self.__url('api_dispatch_list', res_name='eventupdates')


class UsersMixin(object):

    def setUp(self):
        super(UsersMixin, self).setUp()
        self.user = UserFactory()

        self.events_granted_user = UserFactory()
        view_event = Permission.objects.get(codename='view_event')
        view_eventupdate = Permission.objects.get(codename='view_eventupdate')
        self.events_granted_user.user_permissions.add(
            view_event, view_eventupdate)

        self.superuser = UserFactory()
        self.superuser.is_superuser = True
        self.superuser.is_staff = True
        self.superuser.save()

    def _get_client(self):
        if hasattr(self, 'api_client'):
            return self.api_client.client
        else:
            return self.client

    def login_as(self, user):
        client = self._get_client()
        success = client.login(username=user.username,
                               password=user.real_password)
        self.assertTrue(success)

    def login_as_superuser(self):
        self.login_as(self.superuser)

    def login_as_user(self):
        self.login_as(self.user)

    def logout(self):
        self._get_client().logout()
