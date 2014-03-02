from mock import patch

from django.test import TestCase

import events.jabber
from .helpers import UserFactory


class SignalsTestCase(TestCase):

    @patch.object(events.jabber, 'on_user_created')
    def test_on_user_created(self, mock_on_user_created):
        user = UserFactory()
        mock_on_user_created.assert_called_once_with(user)
