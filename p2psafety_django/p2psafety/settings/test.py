from .django import *
from .apps import *
from .site import *
from .local import *

DEBUG = False
TEMPLATE_DEBUG = False

PASSWORD_HASHERS = (
    'django.contrib.auth.hashers.MD5PasswordHasher',
)

SOUTH_TESTS_MIGRATE = False
JABBER_DRY_RUN = True
