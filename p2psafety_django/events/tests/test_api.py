import mock
import tempfile
from operator import itemgetter

from django.core.urlresolvers import reverse
from django.contrib.gis.geos import Point

from tastypie.test import ResourceTestCase

from ..models import Event, EventUpdate
from ..api.resources import EventResource
from .helpers import  mock_get_backend
from .helpers.factories import EventFactory, EventUpdateFactory, UserFactory
from .helpers.mixins import ModelsMixin, UsersMixin


class PermissionTestCase(UsersMixin, ModelsMixin, ResourceTestCase):
    """
    Tests permissions on different RESP methods.
    """
    def login_as_granted_user(self):
        self.login_as(self.events_granted_user)

    @mock_get_backend(module_path='events.api.resources')
    def test_create_events(self):
        """
        Event creation should be public.
        """
        url = self.events_list_url
        data = dict(provider='facebook', access_token='test')
        self.assertHttpCreated(self.api_client.post(url, data=data))

    def test_create_eventupdates(self):
        """
        EventUpdate creation should be public.
        """
        url = self.eventupdates_list_url
        data = dict(key='notexistingkey')
        self.assertHttpNotFound(self.api_client.post(url, data=data))

    def test_get_list_events(self):
        """
        Only users with ``view_event`` permission should have access.
        """
        url = self.events_list_url
        self.assertHttpUnauthorized(self.api_client.get(url, format='json'))
        self.login_as_granted_user()
        self.assertHttpOK(self.api_client.get(url, format='json'))

    def test_get_list_eventupdates(self):
        """
        Only users with ``view_eventupdate`` permission should have access.
        """
        url = self.eventupdates_list_url
        self.assertHttpUnauthorized(self.api_client.get(url, format='json'))
        self.login_as_granted_user()
        self.assertHttpOK(self.api_client.get(url, format='json'))
        self.logout()

        data = dict(event__id=1)
        self.assertHttpUnauthorized(self.api_client.get(url, data=data, format='json'))
        self.login_as_granted_user()
        self.assertHttpOK(self.api_client.get(url, format='json'))


class EventTestCase(ModelsMixin, UsersMixin, ResourceTestCase):

    @mock_get_backend(module_path='events.api.resources')
    def test_create(self):
        url = self.events_list_url

        # no params
        self.assertHttpBadRequest(self.api_client.post(url))

        data = dict(provider='bad_provider', access_token='some_token')

        # bad params
        self.assertHttpBadRequest(self.api_client.post(url, data=data))

        data['provider'] = 'facebook'
        self.assertHttpCreated(self.api_client.post(url, data=data))

        event = Event.objects.latest('id')
        self.assertEqual(event.status, 'P')
        self.assertEqual(event.user, self.auth_user)

        self.assertHttpCreated(self.api_client.post(url, data=data))
        new_event = Event.objects.latest('id')
        self.assertEqual(new_event.status, 'P')
        self.assertEqual(Event.objects.get(id=event.id).status, 'F')
        self.assertEqual(new_event.user, self.auth_user)
        self.assertNotEqual(new_event.PIN, event.PIN)

        user2 = UserFactory()
        self.mocked_get_backend()().do_auth.return_value = user2
        self.assertHttpCreated(self.api_client.post(url, data=data))
        event = Event.objects.latest('id')
        self.assertEqual(event.status, 'P')
        self.assertEqual(event.user, user2)
        self.assertEqual(Event.objects.filter(status='P').count(), 2)
    
    @mock_get_backend(module_path='events.api.resources')
    def test_get_list(self):
        event, event_location = EventFactory(), EventFactory()
        event_updates = [EventUpdateFactory(event=event, location=None),
                         EventUpdateFactory(event=event_location, location=Point(1, 1))]

        self.login_as_superuser()
        resp = self.api_client.get(self.events_list_url, format='json')
        objects = sorted(self.deserialize(resp)['objects'], key=itemgetter('id'))
        self.assertEqual(len(objects), 2)
        self.assertIsNone(objects[0]['latest_location'])
        self.assertIsNotNone(objects[1]['latest_location'])
        self.assertEqual(objects[1]['latest_location'], {'latitude': 1, 'longitude': 1})
        self.assertDictContainsSubset({'id': event.user.id,
                                       'full_name': event.user.username},
                                        objects[0]['user'])
        self.assertDictContainsSubset({'id': event_location.user.id,
                                       'full_name': event_location.user.username},
                                       objects[1]['user'])
        
        for update_dict, update_obj in zip(objects, event_updates):
            latest_update = update_dict.get('latest_update')
            self.assertIsNotNone(latest_update)
            self.assertEqual(latest_update['id'], update_obj.id)


class EventUpdateTestCase(ModelsMixin, UsersMixin, ResourceTestCase):

    def test_create(self):
        url = self.eventupdates_list_url

        # no params
        self.assertHttpBadRequest(self.api_client.post(url))

        # bad key
        data = dict(key='wrong_key')
        self.assertHttpNotFound(self.api_client.post(url, data=data))

        # no additional args
        event = EventFactory()
        data['key'] = event.key
        self.assertHttpBadRequest(self.api_client.post(url, data=data))

        data['text'] = 'emergency'
        self.assertHttpCreated(self.api_client.post(url, data=data))
        eu = EventUpdate.objects.latest('id')
        self.assertEqual(eu.event, event)
        self.assertEqual(eu.text, 'emergency')
        self.assertEqual(eu.event.status, 'A')

        test_lat, test_lon = 1, 1

        # bad location
        data['location'] = dict(latitude=test_lat)
        self.assertHttpBadRequest(self.api_client.post(url, data=data))
        data['location']['longitude'] = test_lon
        self.assertHttpCreated(self.api_client.post(url, data=data))
        eu = EventUpdate.objects.latest('id')
        self.assertEqual(eu.event, event)
        self.assertEqual((eu.location.y, eu.location.x), (test_lat, test_lon))

        with tempfile.TemporaryFile(suffix='.mp3') as f:
            data = {'key': event.key, 'audio': f}
            self.assertHttpCreated(self.api_client.post(url, data=data))
            eu = EventUpdate.objects.latest('id')
            self.assertEqual(eu.event, event)
            self.assertTrue(eu.audio)

        with tempfile.TemporaryFile(suffix='.avi') as f:
            data = {'key': event.key, 'video': f}
            self.assertHttpCreated(self.api_client.post(url, data=data))
            eu = EventUpdate.objects.latest('id')
            self.assertEqual(eu.event, event)
            self.assertTrue(eu.video)

    def test_get_list(self):
        url = self.eventupdates_list_url
        event = EventFactory(user=self.user)
        event_without_updates = EventFactory(user=self.user)
        base = dict(event=event)
        empty = EventUpdateFactory(**base)
        with_text = EventUpdateFactory(text='Text', **base)
        with_location = EventUpdateFactory(location=Point(1, 1), **base)

        self.login_as_superuser()
        resp = self.api_client.get(url, format='json')
        objects = self.deserialize(resp)['objects']
        self.assertEqual(len(objects), 3)

        # Get by event
        resp = self.api_client.get(url, data=dict(event__id=event_without_updates.id))
        objects = self.deserialize(resp)['objects']
        self.assertEqual(len(objects), 0)

        resp = self.api_client.get(url, data=dict(event_id=event.id))
        objects = sorted(self.deserialize(resp)['objects'], key=itemgetter('id'))

        self.assertEqual(len(objects), 3)
        self.assertEqual(objects[1]['text'], 'Text')
        self.assertEqual(objects[2]['location'], dict(longitude=1, latitude=1))
