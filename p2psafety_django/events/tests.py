# -*- coding: utf-8 -*-
import json

from django.test import TestCase
from django.contrib.auth.models import User
from django.core.urlresolvers import reverse

from mock import patch
from factory.django import DjangoModelFactory

from models import Event


class UserFactory(DjangoModelFactory):
    FACTORY_FOR = User


class EventTestCase(TestCase):

    @patch('events.api.resources.get_backend')
    def test_events_post(self, back_mock):
        url = reverse('api_dispatch_list',
            kwargs={'resource_name': 'events', 'api_name': 'v1'})
        user = UserFactory()
        back_mock().social_auth_backend().do_auth.return_value = user

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
        self.assertEqual(resp.status_code, 200)

        event = Event.objects.latest('id')
        self.assertEqual(event.status, 'P')
        self.assertEqual(event.user, user)

        resp = self.client.post(url, data=json.dumps(data), content_type='application/json')
        self.assertEqual(resp.status_code, 200)
        new_event = Event.objects.latest('id')
        self.assertEqual(new_event.status, 'P')
        self.assertEqual(Event.objects.get(id=event.id).status, 'F')
        self.assertEqual(new_event.user, user)


class EventUpdateTestCase(TestCase):
    pass
