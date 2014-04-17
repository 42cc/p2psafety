from django.core.urlresolvers import reverse
from django.test.utils import override_settings

from allauth.socialaccount.models import SocialApp
from tastypie.test import ResourceTestCase


@override_settings(XMPP_SERVER='test.com',
                   XMPP_PUBSUB_SERVER='pubsub.test.com',
                   XMPP_EVENTS_NOTIFICATION_NODE='testnode')
class PublicResourceTestCase(ResourceTestCase):

    @property
    def settings_url(self):
        kwargs = dict(api_name='v1', resource_name='public')
        return reverse('api_public_settings', kwargs=kwargs)

    def test_settings_ok(self):
        url = self.settings_url
        expected_result = {
            u'fb_app_id': None,
            u'xmpp_server': u'test.com',
            u'xmpp_pubsub_server': u'pubsub.test.com',
            u'xmpp_events_notification_node': u'testnode'
        }
        # No facebook app registered
        SocialApp.objects.all().delete()
        resp = self.api_client.get(url)
        self.assertValidJSONResponse(resp)
        self.assertEqual(self.deserialize(resp), expected_result)

        SocialApp.objects.create(provider='facebook', client_id='test_fb')
        expected_result[u'fb_app_id'] = u'test_fb'
        resp = self.api_client.get(url)
        self.assertValidJSONResponse(resp)
        self.assertEqual(self.deserialize(resp), expected_result)

    def test_settings_bad(self):
        url = self.settings_url
        self.assertHttpMethodNotAllowed(self.api_client.post(url))
