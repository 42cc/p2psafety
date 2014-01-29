from tastypie.api import Api

from .resources import EventResource, EventUpdateResource


api_v1 = Api(api_name='v1')
api_v1.register(EventResource())
api_v1.register(EventUpdateResource())