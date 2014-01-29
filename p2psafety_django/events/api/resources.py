# -*- coding: utf-8 -*-
from django.conf import settings
from django.shortcuts import get_object_or_404

from tastypie import http, fields
from tastypie.authentication import Authentication
from tastypie.authorization import Authorization
from tastypie.constants import ALL, ALL_WITH_RELATIONS
from tastypie.resources import ModelResource
from tastypie.validation import Validation
from tastypie.exceptions import ImmediateHttpResponse

from social.backends.utils import get_backend
from social.apps.django_app.utils import load_strategy

from .fields import GeoPointField
from ..models import Event, EventUpdate


class MultipartResource(object):
    """
    This class allows resources to receive files using multipart content type
    """
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
        list_allowed_methods = ['post', 'get']
        fields = ['id', 'status', 'user']
        filtering = {
            'id': ALL,
            'status': ALL,
        }
        detail_allowed_methods = []
        always_return_data = True

    latest_location = GeoPointField('latest_location', null=True, readonly=True)
    latest_update = fields.ForeignKey('events.api.resources.EventUpdateResource',
                                      'latest_update',
                                       full=True, null=True, readonly=True)

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
        bundle.data.update({'user': bundle.obj.user.full_name})
        if 'access_token' in bundle.data:
            del bundle.data['access_token']
        if 'provider' in bundle.data:
            del bundle.data['provider']
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

        return errors


class EventUpdateResource(MultipartResource, ModelResource):
    class Meta:
        queryset = EventUpdate.objects.order_by('-timestamp')
        resource_name = 'eventupdates'
        list_allowed_methods = ['post', 'get']
        detail_allowed_methods = []
        filtering = {'event': ALL_WITH_RELATIONS}
        validation = EventUpdateValidation()
        authentication = Authentication()
        authorization = Authorization()

    location = GeoPointField('location', null=True)
    event = fields.ForeignKey(EventResource, 'event', readonly=True)

    def hydrate(self, bundle):
        key = bundle.data.get('key')
        if key:
            event = get_object_or_404(Event, key=key)
            bundle.obj.event = event

        return bundle