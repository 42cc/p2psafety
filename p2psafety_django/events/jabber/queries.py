import logging

from django.conf import settings

from .clients import get_client


logger = logging.getLogger('events.jabber')


def on_user_created(new_user):
    """
    This function should be called for newly registered user.

    :type new_user: `django.contrib.auth.models.User`
    """
    if not settings.JABBER_DRY_RUN:
        with get_client('UsersClient') as client:
            client.create_account(new_user)


def notify_supporters(event, radius=None):
    """
    Sends notifications to event's supporters via jabber node.

    :type event: :class:`events.models.Event`
    """
    if radius is None:
        radius = settings.XMPP_EVENTS_NOTIFICATION_RADIUS

    if settings.JABBER_DRY_RUN:
        logger.debug('ignored jabber call from "%s"', notify_supporters.__name__)
    else:
        with get_client('EventsNotifierClient') as client:
            client.publish(event, radius)


def notify_supporter(event, supporter):
    raise NotImplementedError
