from django.conf import settings
from django.conf.urls import patterns, include, url
from django.conf.urls.static import static
from django.contrib import admin
from django.contrib.staticfiles.urls import staticfiles_urlpatterns
from django.views.generic import TemplateView

from tastypie.api import Api

from events.api import EventResource, EventUpdateResource
from users.api import UserResource, RoleResource


admin.autodiscover()


api_v1 = Api(api_name='v1')
api_v1.register(EventResource())
api_v1.register(EventUpdateResource())
api_v1.register(UserResource())
api_v1.register(RoleResource())


urlpatterns = patterns('',
    url(r'^$', TemplateView.as_view(template_name='site/index.html'), name='index'),
    url(r'^', include('events.urls', namespace='events')),
    url(r'^admin/', include(admin.site.urls)),
    url(r'^api/', include(api_v1.urls)),
    
    # Delete next line to allow logout confirmation
    url(r'^accounts/logout/$', 'django.contrib.auth.views.logout', {'next_page': '/'}),
    
    url(r'^accounts/', include('allauth.urls')),
)


if settings.DEBUG:
    urlpatterns += staticfiles_urlpatterns()
    urlpatterns += static(
        settings.MEDIA_URL,
        document_root=settings.MEDIA_ROOT
    )
