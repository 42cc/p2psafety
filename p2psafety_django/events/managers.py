from django.db import models


class EventManager(models.Manager):

    def get_current_of(self, user):
        """
        Returns current event for given user. Each user can have multiple ``finished``
        events and one ``active`` or ``passive`` which is the "current".
        
        Raises ``DoesNotExist`` exception if such event not found.
        """
        return self.get(user__id=user.id, status__in=(self.model.STATUS_ACTIVE,
                                                      self.model.STATUS_PASSIVE))
        
