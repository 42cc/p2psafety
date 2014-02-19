import json
import functools

from django import http as django_http

from tastypie import http as tastypie_http
from schematics.exceptions import ValidationError, ModelConversionError


class api_method(object):
    """
    Allows you to map different request methods to different request handlers
    within same url and :class:`tastypie.resource.Resource` method.

    :param url: method's url, will be appended to resource's list endpoint.
    :type url: str or unicode.
    :param name: django url name.
    :type name: str or unicode.

    Usage example::

        @api_method()
        def custom_method(self):
            "Common docstring"
            def get(self, request, *args):
                "Get method docstring"
                pass

            def post(self, request, *args):
                "Post method docstring"
                pass

            return get, post
    """
    def __init__(self, url, name=None):
        self.url = url
        self.name = name
        self.methods = {}
    
    def __call__(self, func):
        methods_funcs = func(None)
        if not isinstance(methods_funcs, (list, tuple)):
            methods_funcs = (methods_funcs,)

        methods_names = [x.func_name.lower() for x in methods_funcs]
        methods = dict(zip(methods_names, methods_funcs))

        @functools.wraps(func)
        def decorated(self, request, *args, **kwargs):
            self.method_check(request, allowed=methods_names)
            self.throttle_check(request)

            method = methods[request.method.lower()]
            try:
                response = method(self, request, *args, **kwargs)
            except django_http.Http404:
                return tastypie_http.HttpNotFound()
            else:
                self.log_throttled_access(request)
                if isinstance(response, tastypie_http.HttpResponse):
                    return response
                
                data = '' if response is None else response
                return self.create_response(request, data)

        decorated.view_url = self.url
        decorated.view_name = self.name
        decorated.view_methods = methods
        return decorated


def body_params(ParamsClass):
    """
    Parses request body with json decoder, validates output against given 
    schematics model and passes result to handler.
    
    :param ParamsClass: validation schema.
    :type ParamsClass: :class:`schematics.models.Model` instance.
    :return: HttpBadRequest  on json or validation errors or handler response.
    """
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
