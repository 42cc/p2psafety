from django.conf import settings
from django.contrib.auth.decorators import login_required, permission_required
from django.core.urlresolvers import reverse
from django.shortcuts import render, redirect

from annoying.decorators import render_to


def login(request):
    url = reverse('social:begin', kwargs={'backend': 'facebook'})
    return redirect(url)


@login_required
@permission_required('events.view_event', raise_exception=True)
@render_to('events/map.html')
def map(request):
    return {
        'GOOGLE_API_KEY': settings.GOOGLE_API_KEY
    }