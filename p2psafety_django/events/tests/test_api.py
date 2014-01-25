from mock import patch
from tempfile import TemporaryFile

from django.core.urlresolvers import reverse
from django.contrib.gis.geos import Point

from tastypie.test import ResourceTestCase

from .helpers import EventFactory, EventUpdateFactory, UserFactory


class EntryResourceTestCase(ResourceTestCase):

    @property
    def events_list_url(self):
        return reverse('api_dispatch_list',
                        kwargs={'resource_name': 'events', 'api_name': 'v1'})

    @patch('events.api.resources.get_backend')
    def test_get_events_list(self, back_mock):
        user = UserFactory()
        back_mock()().do_auth.return_value = user

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