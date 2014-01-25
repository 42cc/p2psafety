# -*- coding: utf-8 -*-


def geo_point(latitude=None, longitude=None):
    return 'POINT (%f %f)' % (float(longitude), float(latitude))


def geo_dict(point):
    return dict(latitude=point.y, longitude=point.x)
