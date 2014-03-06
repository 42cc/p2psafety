from django.contrib.auth.models import User

from allauth.socialaccount.adapter import DefaultSocialAccountAdapter


class SocialAccountAdapter(DefaultSocialAccountAdapter):
    """
    :class:`DefaultSocialAccountAdapter` does not save user's first & last
    names, retrieved from social network.
    """
    def populate_user(self, *args, **kwargs):
        user = super(SocialAccountAdapter, self).populate_user(*args, **kwargs)
        actual_user = User.objects.get(username=user.username)
        actual_user.first_name = user.first_name
        actual_user.last_name = user.last_name
        actual_user.save()
        return user
        
