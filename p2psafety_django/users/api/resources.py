from django import http as django_http
from django.conf.urls import url
from django.contrib.auth.models import User
from django.shortcuts import get_object_or_404

from tastypie import fields, http
from tastypie.resources import ModelResource
from tastypie.utils import trailing_slash

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

    def roles(self, request, pk=None, **kwargs):
        """
        Manages user's roles:

        * For **GET** method, returns user's roles as list of ids.
        * For **POST** method, sets user's roles to given list of ids as ``role_id`` POST param.

        Raises:

        * **403** if ``role_id`` is not found within POST params dict or it is not a list of valid ids.
        * **404** if user is not found.
        """
        self.method_check(request, allowed=['get', 'post'])
        self.throttle_check(request)

        try:
            user = get_object_or_404(User, pk=pk)
        except django_http.Http404:
            return http.HttpNotFound()
        else:
            self.log_throttled_access(request)
            if request.method == 'POST':
                if 'role_id' not in request.POST:
                    return http.HttpBadRequest()

                try:
                    role_ids = map(int, request.POST.getlist('role_id'))
                except ValueError:
                    return http.HttpBadRequest()

                roles = Role.objects.filter(id__in=role_ids)
                user.roles.clear()
                user.roles.add(*roles)
                return http.HttpAccepted()
            else:
                objects = [role.id for role in user.roles.all()]
                return self.create_response(request, objects)


class RoleResource(ModelResource):
    class Meta:
        queryset = Role.objects.all()
        resource_name = 'roles'
        detail_allowed_methods = []
        include_resource_uri = False
