from django.contrib.gis.geos import Point

from tastypie import fields
from tastypie.exceptions import ApiFieldError


class GeoPointField(fields.DictField):
    """
    Converts ``django.contrib.gis.geos.Point`` to dict object.

    Longitude value converts to ``x``, latitude to ``y``.
    """
    def convert(self, value):
        if value is not None:
            return dict(latitude=value.y, longitude=value.x)

    def hydrate(self, bundle):
        location = super(GeoPointField, self).hydrate(bundle)        
        if location is None:
            return location
        
        longitude = location.get('longitude')
        latitude = location.get('latitude')
        if longitude is None or latitude is None:
            raise ApiFieldError('Both longitude and latitude properties are required')

        return Point(longitude, latitude)