from django.conf.urls import patterns, include, url

from tastypie.api import Api

from .api.resources import EventResource, EventUpdateResource


v1_api = Api(api_name='v1')
v1_api.register(EventResource())
v1_api.register(EventUpdateResource())


urlpatterns = patterns('',
    url(r'^$', 'events.views.map'),

    url(r'^api/', include(v1_api.urls)),

    url('', include('social.apps.django_app.urls', namespace='social')),
)
