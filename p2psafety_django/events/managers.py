from django.db import models

from .jabber import EventsNotifier


class EventManager(models.Manager):

    def __init__(self, *args, **kwargs):
        super(EventManager, self).__init__(*args, **kwargs)
        self.events_notifier = EventsNotifier()

    def get_current_of(self, user):
        """
        Returns current event for given user. Each user can have multiple ``finished``
        events and one ``active`` or ``passive`` which is the "current".
        
        Raises ``DoesNotExist`` exception if such event not found.
        """
        return self.get(user__id=user.id, status__in=(self.model.STATUS_ACTIVE,
                                                      self.model.STATUS_PASSIVE))

    def notify_supporters(self, event):
        self.events_notifier.notify_supporters(event)
