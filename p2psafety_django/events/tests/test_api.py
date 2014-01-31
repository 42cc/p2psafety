import tempfile
from operator import itemgetter

from django.core.urlresolvers import reverse
from django.contrib.gis.geos import Point

from tastypie.test import ResourceTestCase

from .helpers import (EventFactory, EventUpdateFactory, UserFactory,
    mock_get_backend)
from ..models import Event, EventUpdate


class ModelsMixin(object):
    @property
    def events_list_url(self):
        return reverse('api_dispatch_list',
            kwargs=dict(resource_name='events', api_name='v1'))

    @property
    def eventupdates_list_url(self):
        return reverse('api_dispatch_list',
            kwargs=dict(resource_name='eventupdates', api_name='v1'))


class UsersTestCase(ModelsMixin, ResourceTestCase):

    def test_get_detail(self):
        user1 = UserFactory(first_name='test', last_name='user')
        user2 = UserFactory(first_name='', last_name='')
        event1, event2 = EventFactory(user=user1), EventFactory(user=user2)

        resp = self.api_client.get(self.events_list_url, format='json')
        objects = sorted(self.deserialize(resp)['objects'], key=itemgetter('id'))
        self.assertEqual(objects[0]['user']['full_name'], 'test user')
        self.assertEqual(objects[1]['user']['full_name'], user2.username)


class EventsTestCase(ModelsMixin, ResourceTestCase):

    @mock_get_backend(module_path='events.api.resources')
    def test_get_list(self):
        event, event_location = EventFactory(), EventFactory()
        event_updates = [
            EventUpdateFactory(event=event, location=None),
            EventUpdateFactory(event=event_location, location=Point(1, 1)),
        ]
        resp = self.api_client.get(self.events_list_url, format='json')

        self.assertValidJSONResponse(resp)
        objects = sorted(self.deserialize(resp)['objects'],
            key=lambda obj: obj['id'])
        self.assertEqual(len(objects), 2)
        self.assertIsNone(objects[0]['latest_location'])
        self.assertIsNotNone(objects[1]['latest_location'])
        self.assertEqual(objects[1]['latest_location'],
            {'latitude': 1, 'longitude': 1})
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

    @mock_get_backend(module_path='events.api.resources')
    def test_create(self):
        # no params
        resp = self.api_client.post(self.events_list_url)
        self.assertEqual(resp.status_code, 400)

        data = dict(provider='bad_provider', access_token='some_token')

        # bad params
        resp = self.api_client.post(self.events_list_url, data=data)
        self.assertEqual(resp.status_code, 400)

        data['provider'] = 'facebook'
        resp = self.api_client.post(self.events_list_url, data=data)
        self.assertEqual(resp.status_code, 201)

        event = Event.objects.latest('id')
        self.assertEqual(event.status, 'P')
        self.assertEqual(event.user, self.auth_user)

        resp = self.api_client.post(self.events_list_url, data=data)
        self.assertEqual(resp.status_code, 201)
        new_event = Event.objects.latest('id')
        self.assertEqual(new_event.status, 'P')
        self.assertEqual(Event.objects.get(id=event.id).status, 'F')
        self.assertEqual(new_event.user, self.auth_user)
        self.assertNotEqual(new_event.PIN, event.PIN)

        user2 = UserFactory()
        self.mocked_get_backend()().do_auth.return_value = user2
        resp = self.api_client.post(self.events_list_url, data=data)
        self.assertEqual(resp.status_code, 201)
        event = Event.objects.latest('id')
        self.assertEqual(event.status, 'P')
        self.assertEqual(event.user, user2)
        self.assertEqual(Event.objects.filter(status='P').count(), 2)


class EventUpdateTestCase(ModelsMixin, ResourceTestCase):

    def test_create(self):
        url = self.eventupdates_list_url

        # no params
        resp = self.api_client.post(url)
        self.assertEqual(resp.status_code, 400)

        # bad key
        data = dict(key='wrong_key')
        resp = self.api_client.post(url, data=data)
        self.assertEqual(resp.status_code, 404)

        # no additional args
        event = EventFactory()
        data['key'] = event.key
        resp = self.api_client.post(url, data=data)
        self.assertEqual(resp.status_code, 400)

        data['text'] = 'emergency'
        resp = self.api_client.post(url, data=data)
        self.assertEqual(resp.status_code, 201)
        eu = EventUpdate.objects.latest('id')
        self.assertEqual(eu.event, event)
        self.assertEqual(eu.text, 'emergency')
        self.assertEqual(eu.event.status, 'A')

        test_lat, test_lon = 1, 1

        # bad location
        data['location'] = dict(latitude=test_lat)
        resp = self.api_client.post(url, data=data)
        self.assertEqual(resp.status_code, 400)

        data['location']['longitude'] = test_lon
        resp = self.api_client.post(url, data=data)
        self.assertEqual(resp.status_code, 201)
        eu = EventUpdate.objects.latest('id')
        self.assertEqual(eu.event, event)
        self.assertEqual((eu.location.y, eu.location.x), (test_lat, test_lon))

        with tempfile.TemporaryFile(suffix='.mp3') as f:
            data = {'key': event.key, 'audio': f}
            resp = self.api_client.post(url, data=data)
            self.assertEqual(resp.status_code, 201)
            eu = EventUpdate.objects.latest('id')
            self.assertEqual(eu.event, event)
            self.assertTrue(eu.audio)

        with tempfile.TemporaryFile(suffix='.avi') as f:
            data = {'key': event.key, 'video': f}
            resp = self.api_client.post(url, data=data)
            self.assertEqual(resp.status_code, 201)
            eu = EventUpdate.objects.latest('id')
            self.assertEqual(eu.event, event)
            self.assertTrue(eu.video)

    def test_retrieve(self):
        url = self.eventupdates_list_url

        event = EventFactory()
        event_without_updates = EventFactory()
        base = dict(event=event)
        empty = EventUpdateFactory(**base)
        with_text = EventUpdateFactory(text='Text', **base)
        with_location = EventUpdateFactory(location=Point(1, 1), **base)

        # Get all
        resp = self.api_client.get(url, format='json')
        self.assertValidJSONResponse(resp)
        objects = self.deserialize(resp)['objects']
        self.assertEqual(len(objects), 3)

        # Get by event
        resp = self.api_client.get(url,
            data=dict(event__id=event_without_updates.id))
        self.assertValidJSONResponse(resp)
        objects = self.deserialize(resp)['objects']
        self.assertEqual(len(objects), 0)

        resp = self.api_client.get(url, data=dict(event_id=event.id))
        self.assertValidJSONResponse(resp)
        objects = sorted(self.deserialize(resp)['objects'],
            key=lambda obj: obj['id'])
        self.assertEqual(len(objects), 3)
        self.assertEqual(objects[1]['text'], 'Text')
        self.assertEqual(objects[2]['location'], dict(longitude=1, latitude=1))
