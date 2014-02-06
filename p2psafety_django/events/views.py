from django.conf import settings
from django.contrib.auth.decorators import login_required, permission_required

from annoying.decorators import render_to


@login_required
@permission_required('events.view_event', raise_exception=True)
@permission_required('events.view_eventupdate', raise_exception=True)
@render_to('events/map.html')
def map(request):
    return {
        'GOOGLE_API_KEY': getattr(settings, 'GOOGLE_API_KEY')
    }
