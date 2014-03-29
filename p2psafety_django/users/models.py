from django.contrib.auth.models import User
from django.db import models
from django.db.models.signals import post_save
from django.dispatch import receiver
from django.utils.translation import ugettext_lazy as _

from phonenumber_field.modelfields import PhoneNumberField


class Profile(models.Model):
    class Meta:
        verbose_name = _('Profile')
        verbose_name_plural = _('Profiles')

    user = models.OneToOneField(User, related_name='profile')
    phone_number = PhoneNumberField(null=True, blank=True)

    def __unicode__(self):
        return u"%s' profile" % self.user.username


User.profile = property(lambda u: Profile.objects.get_or_create(user=u)[0])


@receiver(post_save, sender=User, dispatch_uid='jabber')
def on_user_save(sender, instance, created, **kwargs):
    from events.jabber import on_user_created
    if created:
        on_user_created(instance)


class Role(models.Model):
    """
    Model to define user roles. It can be lawyer, medic, fighter,
    journalist, etc.
    """
    class Meta:
        verbose_name = _('Role')
        verbose_name_plural = _('Roles')

    name = models.CharField(_('name'), max_length=30, unique=True)
    users = models.ManyToManyField(User, related_name='roles', blank=True)

    def __unicode__(self):
        return unicode(self.name)


class MovementType(models.Model):
    """
    Model to define user movement types. It can be car, bike, on foot,
    etc. Feature is defined for map operators to build some expectation
    on activist speed.
    """
    class Meta:
        verbose_name = _('Movement Type')
        verbose_name_plural = _('Movement Types')

    name = models.CharField(_('name'), max_length=30, unique=True)
    users = models.ManyToManyField(User, related_name='movement_types')

    def __unicode__(self):
        return unicode(self.name)

