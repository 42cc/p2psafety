from django.conf import settings
from django.conf.urls import patterns, include, url

from . import views


urlpatterns = patterns('',
    url(r'^map/$', 'events.views.map', name='map'),

    url(r'^map/add_eventupdate/$', views.map_add_eventupdate,
        name='map_add_eventupdate'),
    url(r'^map/close_event/$', views.map_close_event,
        name='map_close_event'),
    url(r'^map/notify_supporters/$', views.map_notify_supporters,
        name='map_notify_supporters'),
)
