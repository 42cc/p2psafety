import simplejson

from django.core.urlresolvers import reverse
from django.http import HttpRequest
from django.template.loader import render_to_string

from docutils.core import Publisher, publish_doctree
from docutils.parsers.rst import Parser
from sphinx.util.compat import Directive


def setup(app):
    app.add_directive('tastydoc', TastyDirective)


class TastyDirective(Directive):
    """
    Sphinx directive for django-tastypie, based on fork:
    
    https://github.com/socialize/django-tastypie
    """

    has_content = True

    def run(self):
        module_parts = self.content[0].split('.')
        module = '.'.join(module_parts[0:len(module_parts) - 1])
        member = module_parts[len(module_parts) - 1]

        api_module = __import__(module, fromlist=['a'])
        api = api_module.__dict__[member]

        parser = Parser()
        publisher = Publisher()
        request = HttpRequest()
        top_level_response = api.top_level(request, None) 
        top_level_doc = simplejson.loads(top_level_response.content)

        for name in sorted(api._registry.keys()):        
            resource = api._registry[name]
            top_level_doc[name]['schema'] = resource.build_schema()
            top_level_doc[name]['docstring'] = resource.__doc__
            list_endpoint = top_level_doc[name]['list_endpoint']
            top_level_doc[name]['extra_actions'] = self.build_extra_actions(resource, list_endpoint)

        output_rst = render_to_string('tastydoc/tastydoc.rst', {'endpoints': top_level_doc})
        doctree = publish_doctree(output_rst)
        return doctree.children

    def build_extra_actions(self, resource, list_endpoint):
        result = {}
        for url in resource.prepend_urls():
            method = url.callback
            method_name = method.__name__
            if hasattr(method, 'view_methods'):
                method_cfg = {
                    'url': self.join_urls(list_endpoint, method.view_url),
                    'description': method.__doc__,
                    'methods': []
                }
                sub_methods = method.view_methods
                for sub_method_name, sub_method in sub_methods.iteritems():
                    method_cfg['methods'].append({
                        'name': sub_method_name,
                        'description': sub_method.__doc__,
                    })
                result[method_name] = method_cfg
        return result

    def join_urls(self, *urls):
        urls = map(lambda s: s.strip('/'), urls)
        urls.insert(0, ''), urls.append('')
        return '/'.join(urls)
