import json
import mock
import tempfile
from operator import itemgetter

from django.contrib.gis.geos import Point

from tastypie.test import ResourceTestCase

from ..models import Event, EventUpdate
from .helpers.factories import EventFactory, EventUpdateFactory, UserFactory
from .helpers.mixins import ModelsMixin, UsersMixin


class PermissionTestCase(UsersMixin, ModelsMixin, ResourceTestCase):
    """
    Tests permissions on different RESP methods.
    """
    def login_as_granted_user(self):
        self.login_as(self.events_granted_user)

    def test_create_events(self):
        """
        Event creation should be public.
        """
        #
        # TODO (dep ticket 52) : test with api key
        #
        pass

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

    required_model_fields = [u'id', u'user', u'type', u'status', u'resource_uri',
                             u'latest_location', u'latest_update']

    def test_create(self):
        url = self.events_list_url
        self.login_as_user()

        self.assertHttpCreated(self.api_client.post(url))

        event = Event.objects.latest('id')
        self.assertEqual(event.status, Event.STATUS_PASSIVE)
        self.assertEqual(event.user, self.user)

        response = self.api_client.post(url)
        self.assertHttpCreated(response)
        self.assertIn('key', json.loads(response.content))
        new_event = Event.objects.latest('id')
        self.assertEqual(new_event.status, Event.STATUS_PASSIVE)
        self.assertEqual(new_event.type, Event.TYPE_VICTIM)
        self.assertEqual(Event.objects.get(id=event.id).status, Event.STATUS_FINISHED)
        self.assertEqual(new_event.user, self.user)
        self.assertNotEqual(new_event.PIN, event.PIN)

        user2 = UserFactory()
        self.logout()
        self.login_as(user2)
        self.assertHttpCreated(self.api_client.post(url))
        event = Event.objects.latest('id')
        self.assertEqual(event.status, Event.STATUS_PASSIVE)
        self.assertEqual(event.user, user2)
        self.assertEqual(Event.objects.filter(status=Event.STATUS_PASSIVE).count(), 2)

    def test_get_list(self):
        event, event_location = EventFactory(), EventFactory()
        event_updates = [EventUpdateFactory(event=event, location=None),
                         EventUpdateFactory(event=event_location, location=Point(1, 1))]

        self.login_as_superuser()
        resp = self.api_client.get(self.events_list_url, format='json')
        objects = sorted(self.deserialize(resp)['objects'], key=itemgetter('id'))
        self.assertEqual(len(objects), 2)

        for event_dict in objects:
            self.assertKeys(event_dict, self.required_model_fields)

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

    @mock.patch('events.models.Event.support_by_user')
    def test_support_good(self, support_by_user_mock):
        user_victim, user_supporter = UserFactory(), self.superuser
        event_victim = EventFactory(user=user_victim)
        event_supporter = EventFactory(user=user_supporter,
                                       status=Event.STATUS_ACTIVE)

        self.login_as_superuser()
        url = self.events_support_url(event_victim.id)
        data = dict(user_id=user_supporter.id)
        resp = self.api_client.post(url, data=data)
        self.assertEqual(resp.status_code, 200)
        support_by_user_mock.assert_called_once_with(user_supporter)

    @mock.patch('events.models.Event.support_by_user')
    def test_support_bad(self, support_by_user_mock):
        user_victim, user_supporter = UserFactory(), self.superuser
        event_victim = EventFactory(user=user_victim)
        
        url = self.events_support_url(event_victim.id)

        # Invalid method
        self.assertHttpMethodNotAllowed(self.api_client.get(url))

        # Invalid body
        self.assertHttpBadRequest(self.api_client.post(url, data='invalid'))

        # Invalid params
        data = dict(user_id='invalid')
        self.assertHttpBadRequest(self.api_client.post(url, data=data))

        # Event does not exists
        data = dict(user_id=user_supporter.id)
        not_found_url = self.events_support_url(123)
        self.assertHttpNotFound(self.api_client.post(not_found_url, data=data))

        # User does not exists
        data = dict(user_id=123)
        self.assertHttpBadRequest(self.api_client.post(url, data=data))


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
