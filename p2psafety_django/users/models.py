from django.contrib.auth.models import User
from django.db import models
from django.utils.translation import ugettext_lazy as _


class Role(models.Model):
    class Meta:
        verbose_name = _('Role')
        verbose_name_plural = _('Roles')

    name = models.CharField(_('name'), max_length=30, unique=True)
    users = models.ManyToManyField(User, related_name='roles')

    def __unicode__(self):
        return unicode(self.name)
