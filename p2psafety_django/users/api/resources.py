from django.contrib.auth.models import User

from tastypie import fields
from tastypie.resources import ModelResource


class UserResource(ModelResource):
    class Meta:
        queryset = User.objects.all()
        resource_name = 'users'
        fields = ['id']

    full_name = fields.CharField('get_full_name')

    def dehydrate_full_name(self, bundle):
        value = bundle.data['full_name']
        return value if value else bundle.obj.username
