from django import http as django_http
from django.conf.urls import url
from django.contrib.auth.models import User
from django.shortcuts import get_object_or_404

from tastypie import fields, http
from tastypie.resources import ModelResource
from tastypie.utils import trailing_slash
from schematics.models import Model as SchemaModel
from schematics.types import IntType
from schematics.types.compound import ListType

from core.api.decorators import api_method, body_params
from ..models import Role


class UserResource(ModelResource):
    class Meta:
        queryset = User.objects.all()
        resource_name = 'users'
        fields = ['id']
        detail_allowed_methods = []
        list_allowed_methods = []
        extra_actions = {
            'roles': {
                'url': '/{userpk}/roles/',
            }
        }

    full_name = fields.CharField('get_full_name')

    def dehydrate_full_name(self, bundle):
        value = bundle.data['full_name']
        return value if value else bundle.obj.username

    def prepend_urls(self):
        return [
            url(r'^(?P<resource_name>%s)/(?P<pk>\d+)/roles%s$' %
                (self._meta.resource_name, trailing_slash()),
                self.wrap_view('roles'), name='api_users_roles'),
        ]

    @api_method
    def roles(self):
        """
        ***
        TODO: replace user with request.user.
        ***

        Manages user's roles.
        Accepts args as json object.

        * For **GET** method, returns user's roles as list of ids.
        * For **POST** method, sets user's roles to given list of ids as ``role_id`` param.

        Raises:

        * **403** if ``role_id`` is not found within params dict or it is not a list of valid ids.
        * **404** if user is not found.
        """

        def get(self, request, pk=None, **kwargs):
            user = get_object_or_404(User, pk=pk)
            objects = [role.id for role in user.roles.all()]
            return self.create_response(request, objects)

        class PostParams(SchemaModel):
            role_ids = ListType(IntType(), required=True)

        @body_params(PostParams)
        def post(self, request, pk=None, params=None, **kwargs):
            user = get_object_or_404(User, pk=pk)
            roles = Role.objects.filter(id__in=params.role_ids)
            user.roles.clear()
            user.roles.add(*roles)
            return http.HttpAccepted()

        return get, post


class RoleResource(ModelResource):
    class Meta:
        queryset = Role.objects.all()
        resource_name = 'roles'
        detail_allowed_methods = []
        include_resource_uri = False
