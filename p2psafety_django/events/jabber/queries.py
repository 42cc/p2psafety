import logging

from django.conf import settings
from django.contrib.auth.models import User

import users.utils
from . import logger
from .clients import PubsubClient


__all__ = ['on_startup', 'on_user_created', 'notify_supporters', 'notify_supporter']


def _get_registered_users():
    """
    Returns registered users as list of jid-s.
    """
    return []


def _create_account(user):
    jabber_username = '%s@p2psafety.net' % user.username
    jabber_password = users.utils.get_api_key(user).key
    return jabber_username


def on_user_created(new_user):
    """
    This function should be called for newly registered user.

    :type new_user: `django.contrib.auth.models.User`
    """
    _create_account(new_user)


def on_startup():
    """
    This function should be called on server start.
    """
    logger.info('jabber account synchronization started')
    jabber_users = _get_registered_users()
    jabber_usernames = [jid.split('@')[0] for jid in jabber_users]
    site_users = User.objects.only('id', 'username')
    created_jids = [_create_account(u) for u in site_users
                    if u.username not in jabber_usernames]
    logger.info('created %d accounts for: %s', len(created_jids), created_jids)


def notify_supporters(event):
    """
    Sends notifications to event's supporters via jabber node.

    :type event: :class:`events.models.Event`
    """
    from events.api.resources import EventResource

    # Constructing payload
    resource = EventResource()
    event_dict = resource.full_dehydrate(resource.build_bundle(obj=event))
    payload = resource.serialize(None, event_dict, 'application/xml')

    with PubsubClient(settings.EVENTS_NOTIFIER) as client:
        client.publish(payload)


def notify_supporter(event, supporter):
    raise NotImplementedError
