from django.test import TestCase

from waffle.models import Switch


class SwithcesTestCase(TestCase):

    fixtures = ['initial_data.json']

    def setUp(self):
        super(SwithcesTestCase, self).setUp()

    def set_switch(self, name, to=True):
        switch = Switch.objects.get(name=name)
        switch.active = to
        switch.save()
