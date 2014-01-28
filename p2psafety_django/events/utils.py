# -*- coding: utf-8 -*-


def geo_point(latitude, longitude):
    """
    Helper to make geo point and not confuse the order of lat. and long.
    """
    return 'POINT (%f %f)' % (float(longitude), float(latitude))


def geo_dict(point):
    return dict(latitude=point.y, longitude=point.x)