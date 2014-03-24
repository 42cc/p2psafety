# -*- coding: utf-8 -*-
from datetime import timedelta

from django.utils import timezone
from p2psafety.celery import app

from .models import Event, EventUpdate


@app.task
def eventupdate_watchdog(event_id,delay_seconds):
    """Task is run with apply_async(delay=delay) and checks
    for new updates during the delay
    """
    delay = timedelta(seconds=delay_seconds)
    event=Event.objects.get(id=event_id)
    if event.status == Event.STATUS_PASSIVE:
        time_pased = timezone.now() - event.latest_update.timestamp
        if time_pased > delay:
            #generate active eventupdate
            #which will trigger event to be active and show the reason
            EventUpdate(event=event,
                    active=True,
                    text="Watchdog alert. User was inactive for %s" %\
                            str(delay)
            ).save() #to call db hooks
        else:
            eventupdate_watchdog.apply_async(
                    (event_id,delay_seconds),countdown=delay_seconds)
