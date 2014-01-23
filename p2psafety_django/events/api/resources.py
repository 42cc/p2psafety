from django.conf import settings

from tastypie.authentication import Authentication
from tastypie.authorization import Authorization
from tastypie.resources import ModelResource
from social.backends.utils import get_backend
from social.apps.django_app.utils import load_strategy

from events.models import Event, EventUpdate


class EventResource(ModelResource):
    class Meta:
        queryset = Event.objects.all()
        resource_name = 'events'
        authentication = Authentication()
        authorization = Authorization()
        list_allowed_methods = ['post', ]
        detail_allowed_methods = []
        always_return_data = True

    def hydrate(self, bundle):
        access_token = bundle.data.get('access_token')
        provider = bundle.data.get('provider')
        if not (provider and access_token):
            pass  # Add some error message + check if provider in AVAILABLE_PROVIDERS

        social_auth_backend = get_backend(settings.AUTHENTICATION_BACKENDS, provider)

        if social_auth_backend and access_token:
            social_auth = social_auth_backend(strategy=load_strategy(
                request=bundle.request,
                backend=provider,
            ))
            try:
                user = social_auth.do_auth(access_token)
                bundle.obj.user = user
            except:
                pass  # Invalid user token
            bundle.data.pop('provider')
            bundle.data.pop('access_token')

        return bundle


class EventUpdateResource(ModelResource):
    class Meta:
        queryset = EventUpdate.objects.all()
        resource_name = 'eventupdates'
        list_allowed_methods = ['post', ]
        detail_allowed_methods = []

    def hydrate(self, bundle):
        key = bundle.data.get('key')
        event = Event.objects.get(key=key)
        bundle.obj.event = event

        return bundle
