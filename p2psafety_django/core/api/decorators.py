import json
from functools import wraps

from django import http as django_http

from tastypie import http as tastypie_http
from schematics.exceptions import ValidationError, ModelConversionError


def api_method(func):
    methods_funcs = func(None)
    if not isinstance(methods_funcs, (list, tuple)):
        methods_funcs = (methods_funcs,)

    methods_names = [x.func_name.lower() for x in methods_funcs]
    methods_map = dict(zip(methods_names, methods_funcs))

    def decorated(self, request, *args, **kwargs):
        self.method_check(request, allowed=methods_names)
        self.throttle_check(request)

        method = methods_map[request.method.lower()]
        try:
            response = method(self, request, *args, **kwargs)
        except django_http.Http404:
            return tastypie_http.HttpNotFound()
        else:
            self.log_throttled_access(request)
            return tastypie_http.HttpResponse() if response is None else response

    return decorated


def json_body(ParamsClass):
    def decorator(func):
        @wraps(func)
        def decorated(self, request, *args, **kwargs):
            try:
                json_data = json.loads(request.body)
                params = ParamsClass(json_data)
                params.validate()
            except (ValueError, ValidationError, ModelConversionError):
                return tastypie_http.HttpBadRequest('Validation error')
            else:
                kwargs['body_params'] = params
                return func(self, request, *args, **kwargs)
        return decorated
    return decorator
