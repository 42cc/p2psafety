import numbers

from django.contrib.gis.geos import Point

from tastypie.test import ResourceTestCase

from ..helpers.factories import EventFactory, EventUpdateFactory
from ...api.resources.jabber import EventResource

class EventTestCase(ResourceTestCase):

    def test_get_object(self):
        event = EventFactory()
        event_update = EventUpdateFactory(event=event, location=Point(1, 1))

        resource = EventResource()
        bundle = resource.build_bundle(obj=event)
        data = resource.full_dehydrate(bundle).data
        self.assertIsInstance(data.get('support'), basestring)
        self.assertIsInstance(data.get('radius'), numbers.Number)
        self.assertEqual(data.get('location'), {'longitude': 1, 'latitude': 1})
