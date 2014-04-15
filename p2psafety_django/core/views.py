from django.conf import settings

from annoying.decorators import ajax_request
from allauth.socialaccount.models import SocialApp


@ajax_request
def server_info(request):
    result = {}
    try:
        app = SocialApp.objects.filter(provider="facebook")[0]
        result['facebook_app_id'] = app.client_id
    except:
        result['facebook_app_id'] = None

    args_to_give = [
        'XMPP_SERVER',
        'XMPP_PUBSUB_SERVER',
        'XMPP_EVENTS_NOTIFICATION_NODE',
    ]

    for arg in args_to_give:
        result[arg.lower()] = getattr(settings, arg)

    return result
