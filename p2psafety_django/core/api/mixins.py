import functools
import inspect
from operator import itemgetter

from django import http as django_http
from django.conf.urls import url

from tastypie import http as tastypie_http
from tastypie.utils import trailing_slash


class api_method(object):
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
                return tastypie_http.HttpResponse() if response is None else response

        decorated.view_url = self.url
        decorated.view_name = self.name
        decorated.view_methods = methods
        return decorated


class ApiMethodsMixin(object):

    def __get_methods(self):
        resource_methods = inspect.getmembers(type(self), inspect.ismethod)
        resource_names = map(itemgetter(0), resource_methods)
        return [n for n in resource_names if not n.startswith('_')]

    def __wrap_view(self, view_name, wrapped):
        wrapper = self.wrap_view(view_name)
        wrapper.view_url = wrapped.view_url
        wrapper.view_name = wrapped.view_name
        wrapper.view_methods = wrapped.view_methods
        return wrapper

    def prepend_urls(self):
        result = super(ApiMethodsMixin, self).prepend_urls()
        for name in self.__get_methods():
            method = getattr(self, name)
            if hasattr(method, 'view_methods'):
                res_name = r'^(?P<resource_name>%s)/' % self._meta.resource_name
                full_url = res_name + method.view_url.strip('/') + trailing_slash()
                wrapped = self.__wrap_view(name, method)
                django_url = url(full_url, wrapped , name=method.view_name)
                result.append(django_url)
        return result


