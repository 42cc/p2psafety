from django import forms
from django.conf import settings

from .models import Profile


class SignupForm(forms.ModelForm):
    class Meta:
        model = Profile
        fields = ('phone_number',)

    def __init__(self, *args, **kwargs):
        super(SignupForm, self).__init__(*args, **kwargs)
        self.fields.keyOrder = (
            'username', 'email', 'phone_number', 'password1', 'password2'
        )

    def save(self, user):
        user.profile.phone_number = self.cleaned_data['phone_number']
        user.profile.save()

        if settings.ACCOUNT_MODERATION_REQUIRED:
            user.is_active = False
            user.save()
