from django.conf import settings
from django.conf.urls import patterns, include, url

from . import views
from .api import api_v1


urlpatterns = patterns('',
    url(r'^$', 'events.views.map'),

    url(r'^api/', include(api_v1.urls)),
    url(r'^login/$', views.login),
    url(r'^logout/$', 'django.contrib.auth.views.logout', {'next_page': settings.LOGOUT_REDIRECT_URL}),

    # Utils
    url(r'^accounts/', include('social.apps.django_app.urls', namespace='social')),
)
