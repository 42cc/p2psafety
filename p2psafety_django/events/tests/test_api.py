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

        events_num = 3
        event_updates = {}
        for i in xrange(events_num):
            event = EventFactory()
            EventUpdateFactory(event=event)
            EventUpdateFactory(event=event, text='Some text')
            EventUpdateFactory(event=event, audio=None)
            EventUpdateFactory(event=event, video=None)
            for j in xrange(i + 1):
                EventUpdateFactory(event=event, location=Point(i, i))

        resp = self.api_client.get(self.events_list_url, format='json')
        self.assertValidJSONResponse(resp)
        data = self.deserialize(resp)
        self.assertEqual(data['meta']['total_count'], event_num)
        for obj, location in zip(data['objects'], [None, Point(1, 1), Point(2, 2)])
            self.assertEqual(obj['latest_location'], location)
