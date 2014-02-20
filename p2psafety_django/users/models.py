from django.contrib.auth.models import User
from django.db import models
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


class Role(models.Model):
    class Meta:
        verbose_name = _('Role')
        verbose_name_plural = _('Roles')

    name = models.CharField(_('name'), max_length=30, unique=True)
    users = models.ManyToManyField(User, related_name='roles')

    def __unicode__(self):
        return unicode(self.name)
