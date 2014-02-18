TASTYPIE_ABSTRACT_APIKEY = False
API_LIMIT_PER_PAGE = 0

SRID = {
    'default': 4326,  # WGS84, stored in database
    'projected': 900913,  # for Spatialite distance calculation
}

# Fill the fields below for facebook auth to work
SOCIAL_AUTH_FACEBOOK_KEY = NotImplemented
SOCIAL_AUTH_FACEBOOK_SECRET = NotImplemented

POSTGIS_TEMPLATE = 'template_postgis'

GOOGLE_API_KEY = NotImplemented
