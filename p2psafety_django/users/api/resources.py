from django.contrib.auth.models import User
from django.shortcuts import get_object_or_404

from tastypie import fields, http
from tastypie.resources import ModelResource
from schematics.models import Model as SchemaModel
from schematics.types import IntType
from schematics.types.compound import ListType

from core.api.mixins import ApiMethodsMixin
from core.api.decorators import body_params, api_method
from ..models import Role


class UserResource(ApiMethodsMixin, ModelResource):
    class Meta:
        queryset = User.objects.all()
        resource_name = 'users'
        fields = ['id']
        detail_allowed_methods = []
        list_allowed_methods = []

    full_name = fields.CharField('get_full_name')

    def dehydrate_full_name(self, bundle):
        value = bundle.data['full_name']
        return value if value else bundle.obj.username

    @api_method(r'/(?P<pk>\d+)/roles', name='api_users_roles')
    def roles(self):
        """
        ***
        TODO: replace user with request.user.
        ***

        Manages user's roles.

        Raises:

        * **404** if user is not found.
        """

        def get(self, request, pk=None, **kwargs):
            """
            Returns user's roles as list of ids.
            """
            user = get_object_or_404(User, pk=pk)
            objects = [role.id for role in user.roles.all()]
            return self.create_response(request, objects)

        class PostParams(SchemaModel):
            role_ids = ListType(IntType(), required=True)

        @body_params(PostParams)
        def post(self, request, pk=None, params=None, **kwargs):
            """
            Sets user's roles to given list of ids as ``role_id`` param.
            """
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
