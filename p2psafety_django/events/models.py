# -*- coding: utf-8 -*-
import uuid
import hmac

from django.db import models
from django.dispatch import receiver
from django.conf import settings
from django.contrib.auth.models import User
from django.contrib.gis.db import models as geomodels
from django.utils import timezone

try:
    from hashlib import sha1
except ImportError:
    import sha
    sha1 = sha.sha


class Event(models.Model):
    """
    Event class.
    """
    STATUS_CHOICES = (
        ('P', 'Passive'),
        ('A', 'Active'),
        ('F', 'Finished'),
    )

    user = models.ForeignKey(User, related_name='events')
    PIN = models.IntegerField(default=0)
    key = models.CharField(max_length=128, blank=True, default='', db_index=True)
    status = models.CharField(max_length=1, choices=STATUS_CHOICES, default='P')

    def __unicode__(self):
        return "{} event by {}".format(self.status, self.user)

    @property
    def latest_location_update(self):
        try:
            return self.updates.filter(location__isnull=False).latest()
        except EventUpdate.DoesNotExist:
            return None

    def save(self, *args, **kwargs):
        """
        Basic save + generator until PIN is unique.
        """
        if not self.key:
            bad_key = True
            while bad_key:
                self.PIN, self.key = self.generate_keys()
                bad_key = self.__class__.objects.filter(PIN=self.PIN).exclude(
                    status='F').exists()

        return super(Event, self).save(*args, **kwargs)

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
    if kwargs.get('created'):
        instance = kwargs.get('instance')
        Event.objects.filter(status__in=['A', 'P']).exclude(
            id=getattr(instance, 'id')).update(status='F')


class EventUpdate(models.Model):
    """
    """
    class Meta:
        get_latest_by = 'timestamp'

    event = models.ForeignKey(Event, related_name='updates')
    timestamp = models.DateTimeField(default=timezone.now())

    text = models.TextField(blank=True)
    location = geomodels.PointField(srid=settings.SRID['default'], blank=True,
        null=True)
    audio = models.FileField(upload_to='audio', blank=True, null=True)
    video = models.FileField(upload_to='video', blank=True, null=True)

    objects = geomodels.GeoManager()
