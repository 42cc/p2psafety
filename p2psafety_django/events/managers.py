from django.db import models


class EventManager(models.Manager):

    def get_current_active_of(self, user):
        """
        Returns current active event for given user. Basically, each user
        should have one.

        Raises ``DoesNotExist`` exception if active event not found.
        """
        return self.get(user__id=user.id, status=self.model.STATUS_ACTIVE)
