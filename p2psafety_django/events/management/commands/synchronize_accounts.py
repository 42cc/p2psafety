from django.core.management.base import BaseCommand

import events.jabber.queries


class Command(BaseCommand):

    help = 'Creates jabber accounts for registered users'

    def handle(self, *args, **kwargs):
        client = events.jabber.queries.get_client('UsersClient')
        with client:
            client.synchronize_accounts()
