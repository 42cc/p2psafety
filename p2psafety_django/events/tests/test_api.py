from tempfile import TemporaryFile

from django.core.urlresolvers import reverse
from django.contrib.gis.geos import Point

from tastypie.test import ResourceTestCase

from .helpers import EventFactory, EventUpdateFactory, UserFactory, mock_get_backend
from ..models import Event, EventUpdate


class ModelsMixin(object):
    @property
    def events_list_url(self):
        return reverse('api_dispatch_list', kwargs=dict(resource_name='events',
                                                        api_name='v1'))

    @property
    def eventupdates_list_url(self):
        return reverse('api_dispatch_list', kwargs=dict(resource_name='eventupdates',
                                                        api_name='v1'))


class EventsTestCase(ModelsMixin, ResourceTestCase):

    @mock_get_backend(module_path='events.api.resources')
    def test_get_list(self):
        event, event_location = EventFactory(), EventFactory()
        EventUpdateFactory(event=event, location=None)
        EventUpdateFactory(event=event_location, location=Point(1, 1))
        resp = self.api_client.get(self.events_list_url, format='json')
        self.assertValidJSONResponse(resp)
        objects = sorted(self.deserialize(resp)['objects'], key=lambda obj: obj['id'])
        self.assertEqual(len(objects), 2)
        self.assertIsNone(objects[0]['latest_location_update'])
        self.assertIsNotNone(objects[1]['latest_location_update'])
        self.assertEqual(objects[1]['latest_location_update']['location'],
                         {u'latitude': 1, u'longitude': 1})

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


class EventUpdateTestCase(ModelsMixin, ResourceTestCase):

    def test_create(self):
        url = self.eventupdates_list_url

        # wrong request type
        resp = self.api_client.get(url)
        self.assertEqual(resp.status_code, 405)

        # no params
        resp = self.api_client.post(url,)
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

        data.update(latitude=50.450731, longitude=30.529487)
        resp = self.api_client.post(url, data=data)
        self.assertEqual(resp.status_code, 201)
        eu = EventUpdate.objects.latest('id')
        self.assertEqual(eu.event, event)
        self.assertEqual((eu.location.y, eu.location.x), (50.450731, 30.529487))

        with TemporaryFile(suffix='.mp3') as f:
            data = {'key': event.key, 'audio': f}
            resp = self.api_client.post(url, data=data)
            self.assertEqual(resp.status_code, 201)
            eu = EventUpdate.objects.latest('id')
            self.assertEqual(eu.event, event)
            self.assertTrue(eu.audio)

        with TemporaryFile(suffix='.avi') as f:
            data = {'key': event.key, 'video': f}
            resp = self.api_client.post(url, data=data)
            self.assertEqual(resp.status_code, 201)
            eu = EventUpdate.objects.latest('id')
            self.assertEqual(eu.event, event)
            self.assertTrue(eu.video)
