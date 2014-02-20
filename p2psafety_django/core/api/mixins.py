import functools
import inspect
from operator import itemgetter

from django import http as django_http
from django.conf.urls import url

from tastypie import http as tastypie_http
from tastypie.utils import trailing_slash


class ApiMethodsMixin(object):
    """
    Add this mixin to :class:`tastypie.resources.Resource` class if you need
    :func:`core.api.decorators.api_method` decorator.
    """
    def __get_methods(self):
        resource_methods = inspect.getmembers(type(self), inspect.ismethod)
        resource_names = map(itemgetter(0), resource_methods)
        return [n for n in resource_names if not n.startswith('_')]

    def __wrap_view(self, view_name, wrapped):
        wrapper = self.wrap_view(view_name)
        functools.update_wrapper(wrapper, wrapped)
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
