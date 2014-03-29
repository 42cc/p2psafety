from django.test import TestCase

from tastypie.models import ApiKey

from .helpers import UserFactory
from .. import utils


class UtilsTestCase(TestCase):

    def test_get_api_key(self):
        user = UserFactory()

        api_key = utils.get_api_key(user)
        self.assertIsInstance(api_key, ApiKey)
        self.assertEqual(api_key.user, user)
