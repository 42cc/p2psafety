from django.conf import settings

from .clients import get_client


__all__ = ['on_user_created', 'notify_supporters', 'notify_supporter']


def on_user_created(new_user):
    """
    This function should be called for newly registered user.

    :type new_user: `django.contrib.auth.models.User`
    """
    if settings.JABBER_ENABLED:
        with get_client('UsersClient') as client:
            client.create_account(new_user)


def notify_supporters(event):
    """
    Sends notifications to event's supporters via jabber node.

    :type event: :class:`events.models.Event`
    """
    if settings.JABBER_ENABLED:
        with get_client('EventsNotifierClient') as client:
            client.publish(event)


def notify_supporter(event, supporter):
    raise NotImplementedError
