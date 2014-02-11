TASTYPIE_ABSTRACT_APIKEY = True
API_LIMIT_PER_PAGE = 0

SRID = {
    'default': 4326,  # WGS84, stored in database
    'projected': 900913,  # for Spatialite distance calculation
}

#
# django-allauth settings
#
# Login method to use
ACCOUNT_AUTHENTICATION_METHOD = 'username'
# Required during registration
ACCOUNT_EMAIL_REQUIRED = True
# Email verification is required
ACCOUNT_EMAIL_VERIFICATION = 'mandatory'
ACCOUNT_SIGNUP_FORM_CLASS = 'users.forms.SignupForm'


POSTGIS_TEMPLATE = 'template_postgis'

GOOGLE_API_KEY = NotImplemented
