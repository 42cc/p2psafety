import mock

from django.contrib.auth.models import User
from django.core.urlresolvers import reverse
from django.test import TestCase

from tastypie.test import ResourceTestCase

from .helpers.mixins import UsersMixin
from .helpers.factories import EventFactory
from ..models import Event, EventUpdate
from .. import jabber
from users.tests.helpers import UserFactory


class ViewsTestCase(UsersMixin, TestCase):

    def test_events_map(self):
        url = reverse('events:map')
        self.assertEqual(self.client.get(url).status_code, 302)
        self.login_as(self.events_granted_user)
        self.assertEqual(self.client.get(url).status_code, 200)


class MapTestCase(UsersMixin, ResourceTestCase):

    def test_add_eventupdate_ok(self):
        event = EventFactory(user=self.user)
        url = reverse('events:map_add_eventupdate')

        self.login_as_superuser()
        data = dict(event_id=event.id, text='test')
        resp = self.api_client.post(url, data=data)
        self.assertValidJSONResponse(resp)
        self.assertTrue(self.deserialize(resp)['success'])

    def test_add_eventupdate_errors(self):
        user, operator = self.user, self.superuser
        event = EventFactory(user=user)
        url = reverse('events:map_add_eventupdate')
        valid_data = dict(event_id=event.id, text='test')

        # No permissions
        self.login_as_user()
        self.assertHttpForbidden(self.api_client.post(url, data=valid_data))

        self.login_as_superuser()
        
        # No text
        data = dict(event_id=event.id)
        self.assertHttpBadRequest(self.api_client.post(url, data=data))

        # Invalid id
        data = dict(valid_data, event_id='test')
        self.assertHttpBadRequest(self.api_client.post(url, data=data))

        # No such event
        data = dict(valid_data, event_id=event.id + 1)
        self.assertHttpNotFound(self.api_client.post(url, data=data))

    def test_close_event_ok(self):
        event = EventFactory(user=self.user)
        url = reverse('events:map_close_event')

        self.login_as_superuser()
        resp = self.api_client.post(url, data=dict(event_id=event.id))
        event = Event.objects.get(id=event.id)
        self.assertValidJSONResponse(resp)
        self.assertTrue(self.deserialize(resp)['success'])
        self.assertEqual(event.status, event.STATUS_FINISHED)

    def test_close_event_errors(self):
        user, operator = self.user, self.superuser
        event = EventFactory(user=user)
        url = reverse('events:map_close_event')
        valid_data = dict(event_id=event.id)

        # No permissions
        self.login_as_user()
        self.assertHttpForbidden(self.api_client.post(url, data=valid_data))

        self.login_as_superuser()

        # Invalid id
        data = dict(valid_data, event_id='test')
        self.assertHttpBadRequest(self.api_client.post(url, data=data))

        # No such event
        data = dict(event_id=event.id + 1)
        self.assertHttpNotFound(self.api_client.post(url, data=data))

    @mock.patch('events.views.jabber')
    def test_notify_supporters_ok(self, mock_jabber):
        user, operator = self.user, self.superuser
        event = EventFactory(user=user)
        url = reverse('events:map_notify_supporters')
        data = dict(event_id=event.id)

        self.login_as_superuser()
        
        # Without radius
        resp = self.api_client.post(url, data=data)
        self.assertValidJSONResponse(resp)
        self.assertTrue(self.deserialize(resp)['success'])
        mock_jabber.notify_supporters.assert_called_once_with(event, radius=None)
        mock_jabber.notify_supporters.reset_mock()

        # With radius
        data['radius'] = 123
        resp = self.api_client.post(url, data=data)
        self.assertValidJSONResponse(resp)
        self.assertTrue(self.deserialize(resp)['success'])
        mock_jabber.notify_supporters.assert_called_once_with(event, radius=123)

    def test_notify_supporters_errors(self):
        user, operator = self.user, self.superuser
        event = EventFactory(user=user)
        url = reverse('events:map_notify_supporters')
        valid_data = dict(event_id=event.id, radius=123)

        # No permissions
        self.login_as_user()
        self.assertHttpForbidden(self.api_client.post(url, data=valid_data))

        self.login_as_superuser()

        # Invalid id
        data = dict(valid_data, event_id='test')
        self.assertHttpBadRequest(self.api_client.post(url, data=data))

        # Invalid radius
        data = dict(valid_data, radius='test')
        self.assertHttpBadRequest(self.api_client.post(url, data=data))

        # No such event
        data = dict(valid_data, event_id=event.id + 1)
        self.assertHttpNotFound(self.api_client.post(url, data=data))

    def test_create_test_event_ok(self):
        url = reverse('events:map_create_test_event')
        users_count, events_count = User.objects.count(), Event.objects.count()
        eventupdates_count = EventUpdate.objects.count()
        data = dict(longitude=1.2, latitude=2.3)
        
        self.login_as_superuser()

        self.assertHttpOK(self.api_client.post(url, data=data))
        self.assertEqual(User.objects.count(), users_count + 1)
        self.assertEqual(Event.objects.count(), events_count + 1)
        self.assertEqual(EventUpdate.objects.count(), eventupdates_count + 1)
        last_update = EventUpdate.objects.latest()
        self.assertNotEqual(last_update.text, '')
        self.assertEqual(last_update.location.x, 1.2)
        self.assertEqual(last_update.location.y, 2.3)

    def test_create_test_event_errors(self):
        url = reverse('events:map_create_test_event')

        # No permissions
        self.login_as_user()
        self.assertHttpForbidden(self.api_client.post(url))

        self.login_as_superuser()

        # Bad request method
        self.assertHttpMethodNotAllowed(self.api_client.get(url))

        # Invalid data
        data = dict(longitude=1)
        self.assertHttpBadRequest(self.api_client.post(url, data=data))

        data = dict(longitude='asd', latitude='dsa')
        self.assertHttpBadRequest(self.api_client.post(url, data=data))
