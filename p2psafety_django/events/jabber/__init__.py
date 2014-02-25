import logging


logger = logging.getLogger('events.jabber')


from .clients import PubsubClient
from .queries import *
