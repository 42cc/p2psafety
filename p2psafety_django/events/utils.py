# -*- coding: utf-8 -*-


def geo_point(latitude=None, longitude=None):
    return 'POINT (%f %f)' % (float(longitude), float(latitude))
