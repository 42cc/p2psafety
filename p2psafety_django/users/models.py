import uuid
import hmac

from django.db import models
from django.contrib.auth.models import User

from tastypie.utils import now

try:
    from hashlib import sha1
except ImportError:
    import sha
    sha1 = sha.sha


class PIN(models.Model):
    """
    ApiKey model for api auth. Basically like tasypie's, but with custom
    generator and active/passive state.
    """
    key = models.CharField(max_length=128, blank=True, default='', db_index=True)
    user = models.ForeignKey(User, related_name='api_key')
    is_active = models.BooleanField(default=True)
    PIN = models.SmallIntegerField(default=0)
    created = models.DateTimeField(default=now)

    def save(self, *args, **kwargs):
        """
        Basic save + generator until PIN is unique between active ones.
        """
        if not self.key:
            bad_key = True
            while bad_key:
                self.PIN, self.key = self.generate_key()
                bad_key = self.objects.filter(PIN=self.PIN).exists()

        return super(PIN, self).save(*args, **kwargs)

    def generate_key(self):
        """
        Generates uuid, and PIN.
        """
        new_uuid = uuid.uuid4()
        PIN = new_uuid.int % 1000000
        key = hmac.new(new_uuid.bytes, digestmod=sha1).hexdigest()
        return (PIN, key)

    @classmethod
    def regenerate(cls, user):
        """
        Invalidates old keys for user and creates a new one.
        """
        old_keys = cls.objects.filter(user=user, is_active=True)
        for key in old_keys:
            key.is_active = False
            key.save()
        cls.objects.create(user=user)



