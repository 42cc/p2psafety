from django.conf import settings
from django.conf.urls import patterns, include, url
from django.conf.urls.static import static
from django.contrib import admin
from django.contrib.staticfiles.urls import staticfiles_urlpatterns
from django.views.generic import TemplateView

from tastypie.api import Api

from events.api.resources import EventResource, EventUpdateResource
from users.api.resources import UserResource, RoleResource, AuthResource, \
                                MovementTypeResource


admin.autodiscover()


api_v1 = Api(api_name='v1')
api_v1.register(EventResource())
api_v1.register(EventUpdateResource())
api_v1.register(UserResource())
api_v1.register(RoleResource())
api_v1.register(MovementTypeResource())
api_v1.register(AuthResource())


urlpatterns = patterns('',
    url(r'^$', TemplateView.as_view(template_name='site/index.html'), name='index'),
    url(r'^', include('events.urls', namespace='events')),
    url(r'^admin/', include(admin.site.urls)),
    url(r'^api/', include(api_v1.urls)),
    url(r'^settings/', include('livesettings.urls')),

    # Delete next line to allow logout confirmation
    url(r'^accounts/logout/$', 'django.contrib.auth.views.logout', {'next_page': '/'}),
    url(r'^accounts/emergency_logout/', 'users.views.emergency_logout', name='emergency_logout'),
    url(r'^accounts/', include('allauth.urls')),
    url(r'^i18n/', include('django.conf.urls.i18n')),
)

if settings.DEBUG:
    urlpatterns += staticfiles_urlpatterns()
    urlpatterns += static(
        settings.MEDIA_URL,
        document_root=settings.MEDIA_ROOT
    )
