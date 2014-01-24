# -*- coding: utf-8 -*-
import json
from tempfile import TemporaryFile

from django.test import TestCase
from django.contrib.auth.models import User
from django.core.urlresolvers import reverse

from mock import patch
import factory

from .models import Event, EventUpdate


class UserFactory(factory.django.DjangoModelFactory):
    FACTORY_FOR = User


class EventFactory(factory.django.DjangoModelFactory):
    FACTORY_FOR = Event

    user = factory.SubFactory(UserFactory)


class EventTestCase(TestCase):

    @patch('events.api.resources.get_backend')
    def test_events_post(self, back_mock):
        url = reverse('api_dispatch_list',
            kwargs={'resource_name': 'events', 'api_name': 'v1'})
        user = UserFactory()
        back_mock()().do_auth.return_value = user

        # wrong request type
        resp = self.client.get(url, content_type='application/json')
        self.assertEqual(resp.status_code, 405)

        # no params
        resp = self.client.post(url, content_type='application/json')
        self.assertEqual(resp.status_code, 400)

        data = {
            'provider': 'bad_provider',
            'access_token': 'some_token',
        }

        # bad params
        resp = self.client.post(url, data=json.dumps(data), content_type='application/json')
        self.assertEqual(resp.status_code, 400)

        data['provider'] = 'facebook'
        resp = self.client.post(url, data=json.dumps(data), content_type='application/json')
        self.assertEqual(resp.status_code, 201)

        event = Event.objects.latest('id')
        self.assertEqual(event.status, 'P')
        self.assertEqual(event.user, user)

        resp = self.client.post(url, data=json.dumps(data), content_type='application/json')
        self.assertEqual(resp.status_code, 201)
        new_event = Event.objects.latest('id')
        self.assertEqual(new_event.status, 'P')
        self.assertEqual(Event.objects.get(id=event.id).status, 'F')
        self.assertEqual(new_event.user, user)
        self.assertNotEqual(new_event.PIN, event.PIN)


class EventUpdateTestCase(TestCase):
    def test_eventsupdate_post(self):
        url = reverse('api_dispatch_list',
            kwargs={'resource_name': 'eventupdates', 'api_name': 'v1'})

        # wrong request type
        resp = self.client.get(url, content_type='application/json')
        self.assertEqual(resp.status_code, 405)

        # no params
        resp = self.client.post(url, content_type='application/json')
        self.assertEqual(resp.status_code, 400)

        # bad key
        data = {
            'key': 'wrong_key'
        }
        resp = self.client.post(url, data=json.dumps(data),
            content_type='application/json')
        self.assertEqual(resp.status_code, 404)

        # no additional args
        event = EventFactory()
        data['key'] = event.key
        resp = self.client.post(url, data=json.dumps(data),
            content_type='application/json')
        self.assertEqual(resp.status_code, 400)

        data['text'] = 'emergency'
        resp = self.client.post(url, data=json.dumps(data),
            content_type='application/json')
        self.assertEqual(resp.status_code, 201)
        eu = EventUpdate.objects.latest('id')
        self.assertEqual(eu.event, event)
        self.assertEqual(eu.text, 'emergency')
        self.assertEqual(eu.event.status, 'A')

        data.update({
            'latitude': 50.450731,
            'longitude': 30.529487,
        })
        resp = self.client.post(url, data=json.dumps(data),
            content_type='application/json')
        self.assertEqual(resp.status_code, 201)
        eu = EventUpdate.objects.latest('id')
        self.assertEqual(eu.event, event)
        self.assertEqual((eu.location.y, eu.location.x), (50.450731, 30.529487))

        with TemporaryFile(suffix='.mp3') as f:
            data = {'key': event.key, 'audio': f}
            resp = self.client.post(url, data=data)
                # content_type='application/json')
            self.assertEqual(resp.status_code, 201)
            eu = EventUpdate.objects.latest('id')
            self.assertEqual(eu.event, event)
            self.assertTrue(eu.audio)

        with TemporaryFile(suffix='.avi') as f:
            data = {'key': event.key, 'video': f}
            resp = self.client.post(url, data=data)
                # content_type='application/json')
            self.assertEqual(resp.status_code, 201)
            eu = EventUpdate.objects.latest('id')
            self.assertEqual(eu.event, event)
            self.assertTrue(eu.video)

