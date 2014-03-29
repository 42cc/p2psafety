import functools

from django import forms
from django.conf import settings

from livesettings import config_value

from .models import Profile


class SignupForm(forms.ModelForm):
    class Meta:
        model = Profile
        fields = ('phone_number',)

    def __init__(self, *args, **kwargs):
        super(SignupForm, self).__init__(*args, **kwargs)
        order = self.fields.keyOrder
        order.remove('phone_number')

        if 'password1' in order:
            insert_at = order.index('password1')
            insert = functools.partial(order.insert, insert_at)
        else:
            insert = order.append

        insert('phone_number')

    def save(self, user):
        user.profile.phone_number = self.cleaned_data['phone_number']
        user.profile.save()

        if config_value('Users', 'user-moderation'):
            user.is_active = False
            user.save()
