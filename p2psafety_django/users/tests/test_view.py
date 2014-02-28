# -*- coding: utf8 -*-
from django.contrib.auth.models import User

from django.test import TestCase
from django.core.urlresolvers import reverse

from .helpers import UserFactory


class UserTestCase(TestCase):
    def test_log_the_fuck_out(self):
        user = UserFactory()
        self.client.login(username=user.username, password=user.username)

        self.assertEqual(user.is_active, True)

        url = reverse('log_the_fuck_out')
        response = self.client.get(url)
        self.assertEqual(response.status_code, 302)

        user = User.objects.get(id=user.id)

        self.assertEqual(user.is_active, False)
