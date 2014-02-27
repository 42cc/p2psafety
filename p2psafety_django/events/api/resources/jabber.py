from django.core.urlresolvers import reverse

from tastypie import fields
from tastypie.resources import ModelResource

from ..fields import GeoPointField
from ...models import Event
from users.api.resources import UserResource


class EventResource(ModelResource):
    class Meta:
        queryset = Event.objects.all()
        fields = ('location', 'support', 'radius')
        include_resource_uri = False

    location = GeoPointField('latest_location', null=True, readonly=True)
    support = fields.CharField(readonly=True)
    radius = fields.CharField(readonly=True)

    def dehydrate_support(self, bundle):
        kwargs = dict(resource_name='events', api_name='v1', pk=bundle.obj.pk)
        support_url = reverse('api_events_support', kwargs=kwargs)
        return support_url

    def dehydrate_radius(self, bundle):
        return 1000


