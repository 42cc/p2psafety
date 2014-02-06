from django.core.urlresolvers import reverse
from django.test import TestCase

from .helpers.mixins import UsersMixin


class ViewsTestCase(UsersMixin, TestCase):

    def test_events_map(self):
        url = reverse('events:map')
        self.assertEqual(self.client.get(url).status_code, 302)
        self.login_as(self.events_granted_user)
        self.assertEqual(self.client.get(url).status_code, 200)