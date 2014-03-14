from django.test import TestCase

from .helpers import MockedEventsNotifierClient
from ...tests.helpers.factories import EventFactory
from users.tests.helpers import UserFactory


class EventsNotifierTestCase(TestCase):

    def test_publish(self):
        user = UserFactory()
        event = EventFactory(user=user)
        client = MockedEventsNotifierClient()
        payload = ("""
            <object>
              <text type="null"></text>
              <support>/api/v1/events/%s/support/</support>
              <radius type="integer">123</radius>
              <user>
                <id type="integer">%s</id>
                <full_name>%s</full_name>
              </user>
              <location type="null"/>
            </object>
            """ % (event.id, user.id, user.username,)
        )

        client.publish(event, 123)
        client.assert_published_once_with(payload)
