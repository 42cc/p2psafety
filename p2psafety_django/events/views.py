import json

from django.conf import settings
from django.contrib.auth.decorators import login_required, permission_required
from django.http import HttpResponseBadRequest
from django.shortcuts import get_object_or_404
from django.views.decorators.http import require_POST
from django.views.decorators.csrf import csrf_exempt

from events.models import EventUpdate, Event

from annoying.decorators import render_to, ajax_request
from livesettings import config_value


@login_required
@permission_required('events.view_event', raise_exception=True)
@permission_required('events.view_eventupdate', raise_exception=True)
@render_to('events/map.html')
def map(request):
    return {
        'GOOGLE_API_KEY': getattr(settings, 'GOOGLE_API_KEY'),
        'newevent_highlight': config_value('EventsMap', 'newevent-highlight'),
        'newevent_sound': config_value('EventsMap', 'newevent-sound'),
        'wakeup_interval': config_value('EventsMap', 'operator-wake-up-alert-interval')
    }


@csrf_exempt
@require_POST
@permission_required('events.add_eventupdate', raise_exception=True)
@ajax_request
def map_add_eventupdate(request):
    try:
        data = json.loads(request.body)
        text = data['text']
        event_id = int(data['event_id'])
    except KeyError, ValueError:
        return HttpResponseBadRequest()
    else:
        event = get_object_or_404(Event, id=event_id)
        EventUpdate.objects.create(user=request.user, event=event, text=text)
        return dict(success=True)


@csrf_exempt
@require_POST
@permission_required('events.change_event', raise_exception=True)
@ajax_request
def map_close_event(request):
    return dict(success=True)
