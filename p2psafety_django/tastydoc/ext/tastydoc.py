import simplejson

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
            resource_dict = top_level_doc[name]
            resource = api._registry[name]
            schema = resource.build_schema()
            resource_dict['schema'] = schema

        output_rst = render_to_string('tastydoc/tastydoc.rst', {'endpoints': top_level_doc})
        doctree = publish_doctree(output_rst)
        return doctree.children
