from django.contrib.auth.models import User
from django.core.urlresolvers import reverse
from django.test import TestCase


from .helpers import UserFactory


class UserTestCase(TestCase):

    def test_emergency_logout(self):
        user = UserFactory()
        url = reverse('emergency_logout')
        
        self.client.login(username=user.username, password=user.username)
        self.assertEqual(user.is_active, True)

        response = self.client.get(url)
        self.assertEqual(response.status_code, 302)

        user = User.objects.get(id=user.id)
        self.assertEqual(user.is_active, False)
