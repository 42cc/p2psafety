# -*- coding: utf-8 -*-
import logging

from django.conf import settings
from django.contrib.auth.models import User
from django.shortcuts import get_object_or_404

from tastypie import http, fields
from tastypie.authentication import MultiAuthentication, ApiKeyAuthentication, \
                                    SessionAuthentication
from tastypie.constants import ALL, ALL_WITH_RELATIONS
from tastypie.exceptions import ImmediateHttpResponse
from tastypie.resources import ModelResource
from tastypie.validation import Validation
from schematics.models import Model as SchemaModel
from schematics.types import IntType

from ..fields import GeoPointField
from ..authorization import CreateFreeDjangoAuthorization
from ...models import Event, EventUpdate
from core.api.mixins import ApiMethodsMixin
from core.api.decorators import body_params, api_method
from users.api.resources import UserResource


logger = logging.getLogger(__name__)


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


class EventResource(ApiMethodsMixin, ModelResource):
    class Meta:
        queryset = Event.objects.all()
        resource_name = 'events'
        authentication = MultiAuthentication(ApiKeyAuthentication(),
                                             SessionAuthentication())
        authorization = CreateFreeDjangoAuthorization()
        list_allowed_methods = ['post', 'get']
        fields = ['id', 'user', 'type', 'status']
        filtering = {
            'id': ALL,
            'status': ALL,
        }
        detail_allowed_methods = []
        always_return_data = True

    user = fields.ForeignKey(UserResource, 'user', full=True, readonly=True)
    type = fields.CharField('get_type_display', readonly=True)
    latest_location = GeoPointField('latest_location', null=True, readonly=True)
    latest_text = fields.CharField('latest_text', null=True, readonly=True)
    latest_update = fields.ForeignKey('events.api.resources.EventUpdateResource',
                                      'latest_update',
                                      full=True, null=True, readonly=True)
    supported = fields.ManyToManyField('events.api.resources.EventResource', 'supported', full=False, readonly=True)

    @api_method(r'/(?P<pk>\d+)/support', name='api_events_support')
    def support(self):
        """
        Marks user as "supporter" for a given event.
        """
        def post(self, request, pk=None, params=None, **kwargs):
            """
            Adds current user to list of event's supporters.
            """
            target_event = get_object_or_404(Event, id=pk)
            target_event.support_by_user(request.user)

        return post

    def hydrate(self, bundle):
        bundle.obj.user = bundle.request.user
        return bundle

    def dehydrate(self, bundle):
        if bundle.request and bundle.request.META.get('REQUEST_METHOD') == 'POST':
            bundle.data['key'] = bundle.obj.key

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
        authentication = MultiAuthentication(ApiKeyAuthentication(),
                                             SessionAuthentication())
        authorization = CreateFreeDjangoAuthorization()

    location = GeoPointField('location', null=True)
    event = fields.ForeignKey(EventResource, 'event', readonly=True)

    def hydrate(self, bundle):
        key = bundle.data.get('key')
        if key:
            event = get_object_or_404(Event, key=key)
            bundle.obj.event = event

        return bundle
