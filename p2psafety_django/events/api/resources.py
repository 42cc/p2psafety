# -*- coding: utf-8 -*-
from django.conf import settings
from django.shortcuts import get_object_or_404

from tastypie import http
from tastypie.authentication import Authentication
from tastypie.authorization import Authorization
from tastypie.resources import ModelResource
from tastypie.validation import Validation
from tastypie.exceptions import ImmediateHttpResponse

from social.backends.utils import get_backend
from social.apps.django_app.utils import load_strategy

from ..models import Event, EventUpdate
from ..utils import geo_point


class MultipartResource(object):
    def deserialize(self, request, data, format=None):
        if not format:
            format = request.META.get('CONTENT_TYPE', 'application/json')

        if format == 'application/x-www-form-urlencoded':
            return request.POST

        if format.startswith('multipart'):
            data = request.POST.copy()
            data.update(request.FILES)
            return data
        return super(MultipartResource, self).deserialize(request, data, format)


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
        return bundle

    def dehydrate(self, bundle):
        bundle.data.pop('provider')
        bundle.data.pop('access_token')
        return bundle


class EventUpdateValidation(Validation):
    def is_valid(self, bundle, request=None):
        if not bundle.data:
            return {'__all__': 'Please add key and other arguments'}

        errors = {}

        key = bundle.data.get('key')
        if not key:
            errors['key'] = 'Key is required'

        if bundle.data.keys() == ['key']:
            errors['__all__'] = 'Additional arguments required'

        latitude = bundle.data.get('latitude')
        longitude = bundle.data.get('longitude')
        if not (latitude and longitude) and (latitude or longitude):
            errors['__all__'] = 'Both latitude and longitude are required'

        return errors


class EventUpdateResource(MultipartResource, ModelResource):
    class Meta:
        queryset = EventUpdate.objects.all()
        resource_name = 'eventupdates'
        list_allowed_methods = ['post', ]
        detail_allowed_methods = []
        validation = EventUpdateValidation()
        authentication = Authentication()
        authorization = Authorization()

    def hydrate(self, bundle):
        key = bundle.data.get('key')
        if key:
            event = get_object_or_404(Event, key=key)
            bundle.obj.event = event

        latitude = bundle.data.get('latitude')
        longitude = bundle.data.get('longitude')
        if latitude and longitude:
            bundle.obj.location = geo_point(latitude=latitude,
                longitude=longitude)

        return bundle
