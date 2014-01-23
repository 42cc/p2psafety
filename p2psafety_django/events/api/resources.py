# -*- coding: utf-8 -*-
from django.conf import settings

from tastypie import http
from tastypie.authentication import Authentication
from tastypie.authorization import Authorization
from tastypie.resources import ModelResource
from tastypie.validation import Validation
from tastypie.exceptions import ImmediateHttpResponse

from social.backends.utils import get_backend
from social.apps.django_app.utils import load_strategy

from events.models import Event, EventUpdate


class EventValidation(Validation):
    def is_valid(self, bundle, request=None):
        if not bundle.data:
            return {'__all__': 'Please add provider and access_token arguments'}

        errors = {}

        provider = bundle.data.get('provider')
        backend = get_backend(settings.AUTHENTICATION_BACKENDS, provider)
        if not backend:
            errors['provider'] = 'This provider is not supported'

        return errors


class EventResource(ModelResource):
    class Meta:
        queryset = Event.objects.all()
        resource_name = 'events'
        authentication = Authentication()
        authorization = Authorization()
        validation = EventValidation()
        list_allowed_methods = ['post', ]
        detail_allowed_methods = []
        always_return_data = True

    def hydrate(self, bundle):
        access_token = bundle.data.get('access_token')
        provider = bundle.data.get('provider')

        social_auth_backend = get_backend(settings.AUTHENTICATION_BACKENDS, provider)

        if social_auth_backend and access_token:
            try:
                social_auth = social_auth_backend(strategy=load_strategy(
                    request=bundle.request,
                    backend=provider,
                ))
                user = social_auth.do_auth(access_token)
                bundle.obj.user = user
            except:
                # log exception here
                raise ImmediateHttpResponse(
                    response=http.HttpBadRequest('Invalid access token'))
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

        # Add hydration for location. It must receive latitude and longitude

        return bundle
