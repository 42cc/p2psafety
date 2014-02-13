import json
import functools

from django import http as django_http

from tastypie import http as tastypie_http
from schematics.exceptions import ValidationError, ModelConversionError


def body_params(ParamsClass):
    def decorator(func):
        @functools.wraps(func)
        def decorated(self, request, *args, **kwargs):
            try:
                json_data = json.loads(request.body)
                params = ParamsClass(json_data)
                params.validate()
            except (ValueError, ValidationError, ModelConversionError):
                return tastypie_http.HttpBadRequest('Validation error')
            else:
                kwargs['params'] = params
                return func(self, request, *args, **kwargs)
        decorated.ParamsClass = ParamsClass
        return decorated
    return decorator
