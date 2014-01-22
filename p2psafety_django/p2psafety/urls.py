from django.conf.urls import patterns, include, url
from django.contrib import admin

from tastypie.api import Api

from users.api.resources import UserResource

v1_api = Api(api_name='v1')
v1_api.register(UserResource())

admin.autodiscover()


urlpatterns = patterns('',
    # Examples:
    # url(r'^$', 'p2psafety_django.views.home', name='home'),
    # url(r'^blog/', include('blog.urls')),

    url(r'^api/', include(v1_api.urls)),
    url(r'^admin/', include(admin.site.urls)),
)
