# -*- coding: utf-8 -*-
import uuid
import hmac

from django.db import models
from django.dispatch import receiver
from django.conf import settings
from django.contrib.auth.models import User
from django.contrib.gis.db import models as geomodels
from django.utils import timezone

import waffle

try:
    from hashlib import sha1
except ImportError:
    import sha
    sha1 = sha.sha


import jabber
from .managers import EventManager


class Event(models.Model):
    """
    Event class.

    Event that receives at least one :class:`EventUpdate` becomes active. User
    can has only one active event at the same time.

    As some events can "support" other events, there are ``supported`` and
    ``supporters`` fields.
    """
    STATUS_ACTIVE = 'A'
    STATUS_PASSIVE = 'P'
    STATUS_FINISHED = 'F'
    STATUS = (
        (STATUS_ACTIVE, 'Active'),
        (STATUS_PASSIVE, 'Passive'),
        (STATUS_FINISHED, 'Finished'),
    )
    TYPE_VICTIM = 0
    TYPE_SUPPORT = 1
    EVENT_TYPE = (
        (TYPE_VICTIM, 'victim'),
        (TYPE_SUPPORT, 'support'),
    )

    class Meta:
        permissions = (
            ("view_event", "Can view event"),
        )

    objects = EventManager()

    user = models.ForeignKey(User, related_name='events')

    PIN = models.IntegerField(default=0)
    key = models.CharField(max_length=128, blank=True, default='', db_index=True)
    status = models.CharField(max_length=1, choices=STATUS, default=STATUS_PASSIVE)
    type = models.IntegerField(choices=EVENT_TYPE, default=TYPE_VICTIM)
    supported = models.ManyToManyField('self', symmetrical=False,
        related_name='supporters', blank=True)

    def __unicode__(self):
        return u"{} event by {}".format(self.status, self.user)

    @property
    def latest_update(self):
        try:
            return self.updates.latest()
        except EventUpdate.DoesNotExist:
            return None

    @property
    def latest_location(self):
        try:
            updates = self.updates.filter(location__isnull=False)
            return updates.latest().location
        except EventUpdate.DoesNotExist:
            return None

    @property
    def related_users(self):
        """
        Returns user ids of self and all related events.
        """
        sd = list(self.supported.all().values_list('user', flat=True))
        ss = list(self.supporters.all().values_list('user', flat=True))
        u = [self.user.id, ]
        return list(u + sd + ss)

    def save(self, *args, **kwargs):
        """
        Basic save + generator until PIN is unique.
        """
        if not self.key:
            bad_key = True
            while bad_key:
                self.PIN, self.key = self.generate_keys()
                bad_key = (Event.objects.filter(PIN=self.PIN)
                                        .exclude(status=self.STATUS_FINISHED)
                                        .exists())
        super(Event, self).save(*args, **kwargs)

    def support_by_user(self, user):
        """
        Binds provided user to this event as "supporter".

        Raises: ``DoesNotExist`` if user has no current event.
        """
        supports_event = Event.objects.get_current_of(user)
        if supports_event.type != self.TYPE_SUPPORT:
            supports_event.type = self.TYPE_SUPPORT
            supports_event.save()

        self.supporters.add(supports_event)

    def notify_supporters(self):
        jabber.notify_supporters(self)

    def generate_keys(self):
        """
        Generates uuid, and PIN.
        """
        new_uuid = uuid.uuid4()
        PIN = new_uuid.int % 1000000
        key = hmac.new(new_uuid.bytes, digestmod=sha1).hexdigest()
        return (PIN, key)


@receiver(models.signals.post_save, sender=Event)
def mark_old_events_as_finished(sender, **kwargs):
    """
    Every user has only one active or passive event. Design decision.
    """
    if kwargs.get('created'):
        event = kwargs['instance']
        (Event.objects.filter(user=event.user)
                      .exclude(status=Event.STATUS_FINISHED)
                      .exclude(id=event.id)
                      .update(status=Event.STATUS_FINISHED))


class EventUpdate(models.Model):
    """
    Event update. Stores any kind of additional information for event.
    """
    class Meta:
        permissions = (
            ("view_eventupdate", "Can view event update"),
        )
        get_latest_by = 'timestamp'

    event = models.ForeignKey(Event, related_name='updates')
    timestamp = models.DateTimeField(default=timezone.now)

    text = models.TextField(blank=True)
    location = geomodels.PointField(srid=settings.SRID['default'], blank=True, null=True)
    audio = models.FileField(upload_to='audio', blank=True, null=True)
    video = models.FileField(upload_to='video', blank=True, null=True)

    objects = geomodels.GeoManager()

    def save(self, *args, **kwargs):
        created = self.pk is None

        if created:
            #
            # Event that received an update becomes active.
            #
            all_events_are_finished = not self.event.user.events.filter(
                status__in=[Event.STATUS_PASSIVE, Event.STATUS_ACTIVE]).exists()
            if self.event.status == Event.STATUS_PASSIVE or all_events_are_finished:
                self.event.status = Event.STATUS_ACTIVE
                self.event.save()
                if waffle.switch_is_active('supporters-autonotify'):
                    self.event.notify_supporters()

            super(EventUpdate, self).save(*args, **kwargs)
