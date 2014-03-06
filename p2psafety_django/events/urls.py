from django.conf import settings
from django.conf.urls import patterns, include, url

from . import views


urlpatterns = patterns('',
    url(r'^map/$', 'events.views.map', name='map'),
    url(r'^map/add_eventupdate/$', views.map_add_eventupdate, name='map_add_eventupdate'),
)
