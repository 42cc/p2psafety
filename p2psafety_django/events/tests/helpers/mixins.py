import mock

from celery import Task

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
        client.logout()
        success = client.login(username=user.username,
                               password=user.real_password)
        self.assertTrue(success)

    def login_as_superuser(self):
        self.login_as(self.superuser)

    def login_as_user(self):
        self.login_as(self.user)

    def logout(self):
        self._get_client().logout()


class CeleryMixin(object):
    """ mock celery
    http://stackoverflow.com/questions/4055860/unit-testing-with-django-celery
    """

    def setUp(self):
        super(CeleryMixin, self).setUp()
        self.applied_tasks = []

        self.task_apply_async_orig = Task.apply_async

        @classmethod
        def new_apply_async(task_class, args=None, kwargs={}, **options):
            return self.handle_apply_async(task_class, args, kwargs, **options)

        # monkey patch the regular apply_sync with our method
        Task.apply_async = new_apply_async

    def tearDown(self):
        super(CeleryMixin, self).tearDown()

        # Reset the monkey patch to the original method
        Task.apply_async = self.task_apply_async_orig

    def handle_apply_async(self, task_class, args=None, kwargs={}, **options):
        self.applied_tasks.append((task_class, tuple(args), kwargs))
        return self.generate_task_id()

    def generate_task_id(self):
        result = mock.Mock()
        result.task_id = 'somestrangenumberfor36lenghtidoneone' 
        return result

    def assert_task_sent(self, task_class, *args, **kwargs):
        nm = lambda n: n.name.split('.')[-1]
        was_sent = any(nm(task_class) == nm(task[0])\
                   and args == task[1]\
                   and kwargs == task[2]
           for task in self.applied_tasks)
        self.assertTrue(was_sent, 'Task not called w/class %s and args %s' % (task_class, args))

    def assert_task_not_sent(self, task_class):
        was_sent = any(task_class.name == task[0].name for task in self.applied_tasks)
        self.assertFalse(was_sent, 'Task was not expected to be called, but was.  Applied tasks: %s' %                 self.applied_tasks)
