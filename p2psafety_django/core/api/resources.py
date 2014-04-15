from django.conf import settings

from allauth.socialaccount.models import SocialApp
from tastypie.resources import Resource

from .decorators import api_method
from .mixins import ApiMethodsMixin

class PublicResource(ApiMethodsMixin, Resource):
    class Meta:
        resource_name = 'public'
        list_allowed_methods = ()
        detail_allowed_methods = ()

    @api_method(r'/settings', name='api_public_settings')
    def settings(self):
        def get(self, request, **kwargs):

            result = dict()
            try:
                fb_app = SocialApp.objects.filter(provider='facebook')[0]
                result['fb_app_id'] = fb_app.client_id
            except IndexError:
                result['fb_app_id'] = None

            for setting in ('XMPP_SERVER', 'XMPP_PUBSUB_SERVER',
                            'XMPP_EVENTS_NOTIFICATION_NODE'):
                result[setting.lower()] = getattr(settings, setting)

            return result

        return get
