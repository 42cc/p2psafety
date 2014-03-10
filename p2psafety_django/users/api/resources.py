from django.contrib.auth import authenticate
from django.contrib.auth.models import User
from django.shortcuts import get_object_or_404

from allauth.socialaccount.providers.facebook.views import fb_complete_login
from tastypie import fields, http
from tastypie.authentication import Authentication, ApiKeyAuthentication, \
                                    SessionAuthentication, MultiAuthentication
from tastypie.models import ApiKey
from tastypie.resources import Resource, ModelResource
from schematics.models import Model as SchemaModel
from schematics.types import IntType, StringType
from schematics.types.compound import ListType

from core.api.mixins import ApiMethodsMixin
from core.api.decorators import body_params, api_method
from .. import utils
from ..models import Role, MovementType


class UserResource(ApiMethodsMixin, ModelResource):
    class Meta:
        queryset = User.objects.all()
        resource_name = 'users'
        fields = ['id']
        detail_allowed_methods = []
        list_allowed_methods = []
        authentication = MultiAuthentication(ApiKeyAuthentication(),
                                             SessionAuthentication())
        include_resource_uri = False

    full_name = fields.CharField('get_full_name')

    def dehydrate_full_name(self, bundle):
        value = bundle.data['full_name']
        return value if value else bundle.obj.username

    @api_method(r'/roles', name='api_users_roles')
    def roles(self):
        """
        Manages user's roles.
        """
        def get(self, request, **kwargs):
            """
            Returns user's roles as list of ids.
            """            
            if request.GET.has_key('id'):
                try:
                    user = User.objects.get(id=request.GET['id'])
                except User.DoesNotExist:
                    return  http.HttpNotFound("User not found")
                except ValueError:
                    return http.HttpBadRequest("Bad user Id")
            else:
                user = request.user

            objects = [role.id for role in user.roles.all()]
            return self.create_response(request, objects)

        class PostParams(SchemaModel):
            role_ids = ListType(IntType(), required=True)

        @body_params(PostParams)
        def post(self, request, params=None, **kwargs):
            """
            Sets user's roles to given list of ids as ``ids`` param.
            """            
            roles = Role.objects.filter(id__in=params.role_ids)
            request.user.roles.clear()
            request.user.roles.add(*roles)
            return http.HttpAccepted()

        return get, post

    @api_method(r'/movement_types', name='api_users_movement_types')
    def movement_types(self):
        """
        Manages user's movement types.
        """
        def get(self, request, **kwargs):
            """
            Returns user's movement types as list of ids.
            """
            if request.GET.has_key('id'):
                try:
                    user = User.objects.get(id=request.GET['id'])
                except User.DoesNotExist:
                    return  http.HttpNotFound("User not found")
                except ValueError:
                    return http.HttpBadRequest("Bad user Id")
            else:
                user = request.user

            objects = [mtype.id for mtype in user.movement_types.all()]
            return self.create_response(request, objects)

        class PostParams(SchemaModel):
            movement_type_ids = ListType(IntType(), required=True) 

        @body_params(PostParams)
        def post(self, request, params=None, **kwargs):
            """
            Sets user's movement types to given list of ids as ``ids`` param.
            """
            mtypes = MovementType.objects.filter(id__in=params.movement_type_ids)
            request.user.movement_types.clear()
            request.user.movement_types.add(*mtypes)
            return http.HttpAccepted()

        return get, post


class RoleResource(ModelResource):
    class Meta:
        queryset = Role.objects.all()
        resource_name = 'roles'
        detail_allowed_methods = []
        list_allowed_methods = ['get']
        include_resource_uri = False
        authentication = MultiAuthentication(ApiKeyAuthentication(),
                                             SessionAuthentication())


class MovementTypeResource(ModelResource):
    class Meta:
        queryset = MovementType.objects.all()
        resource_name = 'movement_types'
        detail_allowed_methods = []
        list_allowed_methods = ['get']
        include_resource_uri = False
        authentication = MultiAuthentication(ApiKeyAuthentication(),
                                             SessionAuthentication())

class AuthResource(ApiMethodsMixin, Resource):
    class Meta:
        resource_name = 'auth'
        authentication = Authentication()
        detail_allowed_methods = []
        list_allowed_methods = []

    def _construct_login_response(self, user):
        return {'username': user.username,
                'key': utils.get_api_key(user).key}

    @api_method(r'/login/site', name='api_auth_login_site')
    def login_with_site(self):
        class SiteLoginParams(SchemaModel):
            username = StringType(required=True)
            password = StringType(required=True)

        @body_params(SiteLoginParams)
        def post(self, request, params=None, **kwargs):
            user = authenticate(username=params.username,
                                password=params.password)
            if user is None:
                return http.HttpUnauthorized('Invalid credentials')

            return self._construct_login_response(user)

        return post

    @api_method(r'/login/(?P<provider>\w+)', name='api_auth_login_social')
    def login_with_social(self):
        class SocialLoginParams(SchemaModel):
            access_token = StringType(required=True)

        @body_params(SocialLoginParams)
        def post(self, request, provider=None, params=None, **kwargs):
            from requests import RequestException
            from allauth.socialaccount import providers
            from allauth.socialaccount.helpers import complete_social_login
            from allauth.socialaccount.models import SocialLogin, SocialToken
            from allauth.socialaccount.providers.facebook.provider import FacebookProvider

            if provider == 'facebook':
                try:
                    app = providers.registry.by_id(FacebookProvider.id).get_app(request)
                    token = SocialToken(app=app, token=params.access_token)
                    login = fb_complete_login(request, app, token)
                    login.token = token
                    login.state = SocialLogin.state_from_request(request)
                    ret = complete_social_login(request, login)
                except RequestException:
                    return http.HttpBadRequest('Error accessing FB user profile')
                else:
                    # If user does not exist
                    if login.account.user.id is None:
                        return http.HttpBadRequest('Not registered')

                    return self._construct_login_response(login.account.user)

            return http.HttpBadRequest('Invalid provider')
        return post
