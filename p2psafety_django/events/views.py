import json
import random

from django.conf import settings
from django.contrib.auth.models import User
from django.contrib.auth.decorators import login_required, permission_required
from django.contrib.gis.geos import Point
from django.http import HttpResponseBadRequest
from django.shortcuts import get_object_or_404
from django.views.decorators.http import require_POST
from django.views.decorators.csrf import csrf_exempt

from .models import EventUpdate, Event
from . import jabber

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
    except (KeyError, ValueError):
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
    try:
        data = json.loads(request.body)
        event_id = int(data['event_id'])
    except (KeyError, ValueError):
        return HttpResponseBadRequest()
    else:
        event = get_object_or_404(Event, id=event_id)
        event.status = Event.STATUS_FINISHED
        event.save()
        return dict(success=True)


@csrf_exempt
@require_POST
@permission_required('events.change_event', raise_exception=True)
@ajax_request
def map_notify_supporters(request):
    try:
        data = json.loads(request.body)
        event_id = int(data['event_id'])
        radius = data.get('radius')
        radius = float(radius) if radius else None
    except (KeyError, ValueError):
        return HttpResponseBadRequest()
    else:
        event = get_object_or_404(Event, id=event_id)
        jabber.notify_supporters(event, radius=radius)
        return dict(success=True)


@csrf_exempt
@require_POST
@permission_required('auth.add_user', raise_exception=True)
@permission_required('events.add_event', raise_exception=True)
@permission_required('events.add_eventupdate', raise_exception=True)
@ajax_request
def map_create_test_event(request):
    user, created = User.objects.get_or_create(username='test_user')
    if not created:
        EventUpdate.objects.filter(event__user=user).delete()
        Event.objects.filter(user=user).delete()

    event = Event.objects.create(user=user)
    point = Point(50, 50)
    kwargs = dict(event=event, location=point, text='Help me!')
    event_update = EventUpdate.objects.create(**kwargs)
    return dict(success=True)
