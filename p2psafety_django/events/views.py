import json
from django.conf import settings
from django.contrib.auth.decorators import login_required, permission_required
from django.views.decorators.http import require_POST
from django.views.decorators.csrf import csrf_exempt
from django.shortcuts import get_object_or_404

from events.models import EventUpdate, Event

from livesettings import config_value

from annoying.decorators import render_to, ajax_request


@login_required
@permission_required('events.view_event', raise_exception=True)
@permission_required('events.view_eventupdate', raise_exception=True)
@render_to('events/map.html')
def map(request):
    return {
        'GOOGLE_API_KEY': getattr(settings, 'GOOGLE_API_KEY'),
        "TIME_ALERT": config_value('Events', 'operator_wake_up_alert_interval')
    }


@csrf_exempt
@login_required
@require_POST
@ajax_request
def operator_add_eventupdate(request):
    data = json.loads(request.body)
    text = data['text']
    event_id = data['event_id']

    event = get_object_or_404(Event, id=event_id)
    EventUpdate.objects.create(user=request.user, event=event, text=text)
    return {'success': True}
