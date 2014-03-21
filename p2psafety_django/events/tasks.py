# -*- coding: utf-8 -*-
import time
from datetime import timedelta

from django.utils import timezone
from p2psafety.celery import app

from .models import Event, EventUpdate


@app.task
def eventupdate_watchdog(event_id,delay_seconds):
    delay = timedelta(seconds=delay_seconds)
    event=Event.objects.get(id=event_id)
    while event.status == Event.STATUS_PASSIVE:
        event=Event.objects.get(id=event_id) #reload each time
        time_pased = timezone.now() - event.latest_update.timestamp
        if time_pased > delay:
            #generate auto update active alert
            #which will trigger event to be active and show the reason
            EventUpdate(event=event,
                    active=True,
                    text="Watchdog alert. User was inactive for %s" %\
                            str(delay)
            ).save() #to call db hooks
        time.sleep(delay_seconds)

