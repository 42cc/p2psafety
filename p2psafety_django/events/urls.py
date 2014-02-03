from django.conf import settings
from django.conf.urls import patterns, include, url


urlpatterns = patterns('',
    url(r'^map/$', 'events.views.map', name='map'),
)
