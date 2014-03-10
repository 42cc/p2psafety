# -*- coding: utf-8 -*-
"""
Django settings for p2psafety project.

For more information on this file, see
https://docs.djangoproject.com/en/1.6/topics/settings/

For the full list of settings and their values, see
https://docs.djangoproject.com/en/1.6/ref/settings/
"""

# Build paths inside the project like this: os.path.join(BASE_DIR, ...)
import os
from .utils import root, proj
BASE_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), '../..'))

# SECURITY WARNING: don't run with debug turned on in production!
DEBUG = True
TEMPLATE_DEBUG = True

ALLOWED_HOSTS = []

# Internationalization
# https://docs.djangoproject.com/en/1.6/topics/i18n/

LANGUAGE_CODE = 'en-us'
TIME_ZONE = 'UTC'
USE_I18N = True
USE_L10N = True
USE_TZ = True

LANGUAGES = (
    ('en', u'English'),
    ('uk', u'Українська'),
    ('pl', u'Polski'),
)
LOCALE_PATHS = (
    root('locale'),
)

# Database
# https://docs.djangoproject.com/en/1.6/ref/settings/#databases

DATABASES = {
    'default': {
        'ENGINE': 'django.contrib.gis.db.backends.postgis',
        'NAME': 'p2psafety',
        'USER': 'vagrant',
        'PASSWORD': 'vagrant',
        'HOST': '127.0.0.1',
        'PORT': '',
    }
}

AUTHENTICATION_BACKENDS = (
    'django.contrib.auth.backends.ModelBackend',
    'allauth.account.auth_backends.AuthenticationBackend',
)

MIDDLEWARE_CLASSES = (
    'django.contrib.sessions.middleware.SessionMiddleware',
    'django.middleware.locale.LocaleMiddleware',
    'django.middleware.common.CommonMiddleware',
    'django.middleware.csrf.CsrfViewMiddleware',
    'django.contrib.auth.middleware.AuthenticationMiddleware',
    'django.contrib.messages.middleware.MessageMiddleware',
    'django.middleware.clickjacking.XFrameOptionsMiddleware',
)

INSTALLED_APPS = (
    'django.contrib.admin',
    'django.contrib.auth',
    'django.contrib.contenttypes',
    'django.contrib.gis',
    'django.contrib.messages',
    'django.contrib.sessions',
    'django.contrib.sites',
    'django.contrib.staticfiles',

    'south',
    'livesettings',

    'core',
    'events',
    'users',
    'tastydoc',

    'allauth',
    'allauth.account',
    'allauth.socialaccount',
    'allauth.socialaccount.providers.facebook',
    # TODO: wait for implementation on mobile apps 
    # 'allauth.socialaccount.providers.google',
    'bootstrap3',
    'tastypie',
)

SITE_ID = 1

ROOT_URLCONF = 'p2psafety.urls'

WSGI_APPLICATION = 'p2psafety.wsgi.application'


# Static files (CSS, JavaScript, Images)
# https://docs.djangoproject.com/en/1.6/howto/static-files/

STATIC_ROOT = root('static_media', 'static')

STATIC_URL = '/static/'
STATICFILES_DIRS = (
    root('static'),
)

MEDIA_URL = '/media/uploads/'
MEDIA_ROOT = root('static_media', 'uploads')

TEMPLATE_DIRS = (
    root('templates'),
)
TEMPLATE_CONTEXT_PROCESSORS = (
    # Django defaults
    'django.contrib.auth.context_processors.auth',
    'django.core.context_processors.debug',
    'django.core.context_processors.i18n',
    'django.core.context_processors.media',
    'django.core.context_processors.request',
    'django.core.context_processors.static',
    'django.core.context_processors.tz',
    'django.contrib.messages.context_processors.messages',

    # django-allauth
    'allauth.account.context_processors.account',
    'allauth.socialaccount.context_processors.socialaccount',
)

# Quick-start development settings - unsuitable for production
# See https://docs.djangoproject.com/en/1.6/howto/deployment/checklist/

SECRET_KEY = 'CHANGE_THIS_KEY_BEFORE_USING_ON_PRODUCTION'

TEST_RUNNER = 'p2psafety.utils.TestRunner'

LOGGING = {
    'version': 1,
    'disable_existing_loggers': True,
    'formatters': {
        'verbose': {
            'format': '%(levelname)s %(asctime)s %(module)s %(process)d %(thread)d %(message)s'
        },
        'simple': {
            'format': '[%(levelname)s][%(asctime)s]: %(message)s'
        },
    },
    'handlers': {
        'mail_admins': {
            'level': 'ERROR',
            'class': 'django.utils.log.AdminEmailHandler',
        },
        'console': {
            'level': 'DEBUG',
            'class': 'logging.StreamHandler',
            'formatter': 'simple',
        },
        'jabber': {
            'level': 'DEBUG',
            'class': 'logging.FileHandler',
            'filename': proj('jabber.log'),
            'formatter': 'simple',
        }
    },
    'loggers': {
        'django.request': {
            'handlers': ['mail_admins'],
            'level': 'ERROR',
            'propagate': False,
        },
        'events': {
            'handlers': ['mail_admins'],
            'level': 'ERROR',
            'propagate': False,
        },
        'events.jabber': {
            'handlers': ['console', 'jabber'],
            'level': 'DEBUG',
            'propagate': False,
        },
    }
}
