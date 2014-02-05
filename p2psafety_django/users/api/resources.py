from django import http as django_http
from django.conf.urls import url
from django.contrib.auth.models import User
from django.shortcuts import get_object_or_404

from tastypie import exceptions, fields, http
from tastypie.authorization import Authorization
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

    full_name = fields.CharField('get_full_name')

    def dehydrate_full_name(self, bundle):
        value = bundle.data['full_name']
        return value if value else bundle.obj.username

    def prepend_urls(self):
        return [
            url(r'^(?P<resource_name>%s)/(?P<pk>\d+)/role/(?P<role_pk>\d+)%s$' % 
                (self._meta.resource_name, trailing_slash()),
                self.wrap_view('role'), name='api_users_role'),
        ]

    def role(self, request, pk=None, role_pk=None, **kwargs):
        if request.method not in ('POST', 'DELETE'):
            return http.HttpMethodNotAllowed()

        self.log_throttled_access(request)
        try:
            user = get_object_or_404(User, pk=pk)
            role = get_object_or_404(Role, pk=role_pk)
        except django_http.Http404:
            return http.HttpNotFound()
        else:
            if request.method == 'POST':
                user.roles.add(role)
                return http.HttpAccepted()
            else:
                user.roles.remove(role)
                return http.HttpNoContent()


class RoleResource(ModelResource):
    class Meta:
        queryset = Role.objects.all()
        resource_name = 'roles'
        detail_allowed_methods = []

    users = fields.ToManyField(UserResource, 'users', full=True)
