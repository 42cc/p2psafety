from django.conf.urls import patterns, include, url
from django.contrib import admin
from django.contrib.staticfiles.urls import staticfiles_urlpatterns
from django.conf import settings
from django.conf.urls.static import static


admin.autodiscover()


urlpatterns = patterns('',
    # Examples:
    # url(r'^$', 'p2psafety_django.views.home', name='home'),
    # url(r'^blog/', include('blog.urls')),

    url(r'^', include('events.urls')),
    url(r'^admin/', include(admin.site.urls)),
)


if settings.DEBUG:
    urlpatterns += staticfiles_urlpatterns()
    urlpatterns += static(
        settings.MEDIA_URL,
        document_root=settings.MEDIA_ROOT
    )

