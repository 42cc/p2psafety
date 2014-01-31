from django.core.urlresolvers import reverse

from .factories import UserFactory


class ModelsMixin(object):
    @property
    def events_list_url(self):
        return reverse('api_dispatch_list', kwargs=dict(resource_name='events',
                                                        api_name='v1'))

    @property
    def eventupdates_list_url(self):
        return reverse('api_dispatch_list', kwargs=dict(resource_name='eventupdates',
                                                        api_name='v1'))

class UsersMixin(object):

    def setUp(self):
        super(UsersMixin, self).setUp()
        self.superuser = UserFactory()
        self.superuser.is_superuser = True; self.superuser.is_staff = True
        self.superuser.save()
        self.user = UserFactory()

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