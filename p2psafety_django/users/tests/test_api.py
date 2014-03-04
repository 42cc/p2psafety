# -*- coding: utf8 -*-
import mock
from operator import itemgetter
from requests import RequestException

from django.contrib.auth.models import User
from django.core.urlresolvers import reverse

from allauth.tests import MockedResponse
from allauth.account.models import EmailAddress
from allauth.socialaccount.models import SocialAccount
from allauth.socialaccount.providers.facebook.provider import FacebookProvider
from allauth.socialaccount.tests import create_oauth_tests
from tastypie.models import ApiKey
from tastypie.test import ResourceTestCase

from .helpers import ModelsMixin, SocialTestCase, api_key_auth as auth, \
                     UserFactory, RoleFactory, MovementTypeFactory 


class PermissionTestCase(ModelsMixin, ResourceTestCase):

    def setUp(self):
        super(PermissionTestCase, self).setUp()
        self.user = UserFactory()

    def test_get_users_list(self):
        self.assertHttpMethodNotAllowed(self.api_client.get(self.users_list_url))

    def test_get_users_detail(self):
        url = self.users_detail_url(self.user.id)
        self.assertHttpMethodNotAllowed(self.api_client.get(url))

    def test_get_list_roles(self):
        url = self.roles_list_url
        self.assertHttpUnauthorized(self.api_client.get(url))
        self.assertHttpOK(self.api_client.get(url, format='json', **auth(self.user)))

    def test_post_list_roles(self):
        url = self.roles_list_url
        self.assertHttpMethodNotAllowed(self.api_client.post(url))

    def test_role_add_remove(self):
        url, data = self.users_roles_url, dict(role_ids=[])
        self.assertHttpUnauthorized(self.api_client.post(url, data=data))
        self.assertHttpAccepted(self.api_client.post(url, data=data, **auth(self.user)))

    def test_get_list_movement_types(self):
        url = self.movement_types_list_url
        self.assertHttpUnauthorized(self.api_client.get(url))
        self.assertHttpOK(self.api_client.get(url, **auth(self.user)))

    def test_post_list_movement_types(self):
        url = self.movement_types_list_url
        self.assertHttpMethodNotAllowed(self.api_client.post(url))

    def test_movement_type_add_remove(self):
        url, data = self.users_movement_types_url, dict(movement_type_ids=[])
        self.assertHttpUnauthorized(self.api_client.post(url, data=data))
        self.assertHttpAccepted(self.api_client.post(url, data=data, **auth(self.user)))


class UsersRolesTestCase(ModelsMixin, ResourceTestCase):

    def test_get_roles(self):
        user = UserFactory()
        url = self.users_roles_url
        role1, role2 = RoleFactory(), RoleFactory()
        user.roles.add(role1)

        resp = self.api_client.get(url, format='json', **auth(user))
        self.assertValidJSONResponse(resp)
        roles_list = self.deserialize(resp)
        self.assertEqual(roles_list, [role1.id])

    def test_set_roles(self):
        # User has 1100
        user = UserFactory()
        role0, role1, role2, role3 = RoleFactory(), RoleFactory(), RoleFactory(), RoleFactory()
        user.roles.add(role0, role1)
        url = self.users_roles_url
        
        # Setting 0110
        data = {'role_ids': [role1.id, role2.id]}
        
        # Results in 0110
        resp = self.api_client.post(url, data=data, **auth(user))
        self.assertEqual(resp.status_code, 202)
        self.assertEqual(data['role_ids'], [r.id for r in user.roles.all()])

    def test_set_single_role(self):
        user, role = UserFactory(), RoleFactory()
        url = self.users_roles_url

        resp = self.api_client.post(url, data=dict(role_ids=role.id), **auth(user))
        self.assertEqual(resp.status_code, 202)
        self.assertEqual(list(user.roles.all()), [role])

    def test_clear_roles(self):
        user, role = UserFactory(), RoleFactory()
        url = self.users_roles_url
        user.roles.add(role)

        resp = self.api_client.post(url, data=dict(role_ids=[]), **auth(user))
        self.assertEqual(resp.status_code, 202)
        self.assertEqual(user.roles.count(), 0)

    def test_role_errors(self):
        user, role  = UserFactory(), RoleFactory()        
        url = self.users_roles_url

        # No ``role_ids`` supplied
        resp = self.api_client.post(url, data={}, **auth(user))
        self.assertEqual(resp.status_code, 400)

        # Invalid body
        resp = self.api_client.post(url, data='invalid data', **auth(user))
        self.assertEqual(resp.status_code, 400)

    def test_roles_by_id(self):
        user1, user2 = UserFactory(),UserFactory()
        url = self.users_roles_url
        role1, role2 = RoleFactory(), RoleFactory()
        user1.roles.add(role1)
        user2.roles.add(role2)

        data = {'id':user1.id, 'format':'json'}
        resp = self.api_client.get(url, data=data, **auth(user1))
        self.assertValidJSONResponse(resp)
        roles_list = self.deserialize(resp)
        self.assertEqual(roles_list, [role1.id])

        data = {'id':user2.id, 'format':'json'}
        resp = self.api_client.get(url, data=data, **auth(user1))
        self.assertValidJSONResponse(resp)
        roles_list = self.deserialize(resp)
        self.assertEqual(roles_list, [role2.id])



class UsersMovementTypesTestCase(ModelsMixin, ResourceTestCase):

    def test_get_movement_types(self):
        user = UserFactory()
        mtype1, mtype2 = MovementTypeFactory(), MovementTypeFactory()
        user.movement_types.add(mtype1)
        url = self.users_movement_types_url

        resp = self.api_client.get(url, format='json', **auth(user))
        self.assertValidJSONResponse(resp)
        movement_types_list = self.deserialize(resp)
        self.assertEqual(movement_types_list, [mtype1.id])

    def test_set_movement_types(self):
        # User has 1100
        user = UserFactory()
        mtype0, mtype1, mtype2, mtype3 = map(lambda i: MovementTypeFactory(), range(4))
        user.movement_types.add(mtype0, mtype1)
        url = self.users_movement_types_url
        
        # Setting 0110
        data = {'movement_type_ids': [mtype1.id, mtype2.id]}
        
        # Results in 0110
        resp = self.api_client.post(url, data=data, **auth(user))
        self.assertEqual(resp.status_code, 202)
        self.assertEqual(data['movement_type_ids'],
                         [m.id for m in user.movement_types.all()])

    def test_set_single_movement_type(self):
        user, mtype = UserFactory(), MovementTypeFactory()
        url = self.users_movement_types_url

        data = dict(movement_type_ids=mtype.id)
        resp = self.api_client.post(url, data=data, **auth(user))
        self.assertEqual(resp.status_code, 202)
        self.assertEqual(list(user.movement_types.all()), [mtype])

    def test_clear_movement_types(self):
        user, mtype = UserFactory(), MovementTypeFactory()
        url = self.users_movement_types_url
        user.movement_types.add(mtype)

        data = dict(movement_type_ids=[])
        resp = self.api_client.post(url, data=data, **auth(user))
        self.assertEqual(resp.status_code, 202)
        self.assertEqual(user.movement_types.count(), 0)

    def test_movement_types_errors(self):
        user, mtype = UserFactory(), MovementTypeFactory()
        url = self.users_movement_types_url
        
        # No ``movement_types_ids`` supplied
        resp = self.api_client.post(url, data={}, **auth(user))
        self.assertEqual(resp.status_code, 400)

        # Invalid body
        resp = self.api_client.post(url, data='invalid data', **auth(user))
        self.assertEqual(resp.status_code, 400)      


class RolesTestCase(ModelsMixin, ResourceTestCase):

    def test_get_list(self):
        user = UserFactory()
        role1, role2 = RoleFactory(), RoleFactory()

        resp = self.api_client.get(self.roles_list_url, **auth(user))
        self.assertValidJSONResponse(resp)
        roles_dicts = sorted(self.deserialize(resp)['objects'], key=itemgetter('id'))
        self.assertEqual(dict(id=role1.id, name=role1.name), roles_dicts[0])
        self.assertEqual(dict(id=role2.id, name=role2.name), roles_dicts[1])


class MovementTypesTestCase(ModelsMixin, ResourceTestCase):

    def test_get_list(self):
        user = UserFactory()
        mtype1, mtype2 = MovementTypeFactory(), MovementTypeFactory()

        resp = self.api_client.get(self.movement_types_list_url, format='json', **auth(user))
        self.assertValidJSONResponse(resp)
        movementtypes_dicts = sorted(self.deserialize(resp)['objects'], key=itemgetter('id'))
        self.assertEqual(dict(id=mtype1.id, name=mtype1.name), movementtypes_dicts[0])
        self.assertEqual(dict(id=mtype2.id, name=mtype2.name), movementtypes_dicts[1])

        

class AuthTestCase(SocialTestCase):

    def setUp(self):
        super(AuthTestCase, self).setUp()
        self.user = UserFactory()

    def create_mocked_fb_response(self):
        uid = u'100006489630240'
        email = 'test@mail.com'
        return MockedResponse(200, u"""{
          "id": "%s",
          "name": "Dmytro  Ferens",
          "first_name": "Dmytro",
          "last_name": "Ferens",
          "email": "%s",
          "link": "https://www.facebook.com/ferensdima",
          "hometown": {
            "id": "112795968731448",
            "name": "Krasyliv"
          },
          "location": {
            "id": "111227078906045",
            "name": "Kyiv,Ukraine"
          },
          "education": [
            {
              "school": {
                "id": "184829834894371",
                "name": "НТУУ КПІ/NTUU KPI/НТУУ КПИ"
              },
              "type": "College"
            }
          ],
          "gender": "male",
          "timezone": 2,
          "locale": "ru_RU",
          "verified": true,
          "updated_time": "2013-09-22T10:58:22+0000",
          "username": "ferensdima"
        }""" % (uid, email)), uid, email

    def test_login_with_site(self):
        url = reverse('api_auth_login_site', kwargs={'resource_name': 'auth',
                                                     'api_name': 'v1'})
        # Wrong method
        self.assertHttpMethodNotAllowed(self.api_client.get(url))

        # Invalid data
        self.assertHttpBadRequest(self.api_client.post(url))
        
        # Didnt pass validation
        data = dict(username=None, password=None)
        self.assertHttpBadRequest(self.api_client.post(url, data=data))

        # Valid data
        data = dict(username=self.user.username,
                    password=self.user.real_password)
        resp = self.api_client.post(url, data=data, format='json')
        self.assertHttpOK(resp)

    def test_login_with_social_facebook(self):
        kwargs = dict(resource_name='auth', api_name='v1', provider='facebook')
        url = reverse('api_auth_login_social', kwargs=kwargs)

        # Wrong method
        self.assertHttpMethodNotAllowed(self.api_client.get(url))

        # Invalid data
        data = dict()
        self.assertHttpBadRequest(self.api_client.post(url, data=data))
        
        # Invalid token
        data = dict(access_token='invalid')
        with mock.patch('users.api.resources.fb_complete_login') as fb_complete_mock:
            fb_complete_mock.side_effect = RequestException()
            resp = self.api_client.post(url, data=data)
            self.assertHttpBadRequest(resp)
            self.assertIn('error accessing', resp.content.lower())

        # Valid token, user is not registered
        data = dict(access_token='mocked token')
        mocked_resp, mocked_uid, mocked_email = self.create_mocked_fb_response()
        with mock.patch('allauth.socialaccount.providers.facebook.views.'
                        'requests') as requests_mock:
            requests_mock.get.return_value.json.return_value = mocked_resp.json()

            resp = self.api_client.post(url, data=data)
            self.assertHttpBadRequest(resp)
            self.assertIn('not registered', resp.content.lower())
        
        # Valid token, user is registered
        user = UserFactory(username='ferensdima')        
        email = EmailAddress.objects.create(user=user, email=mocked_email,
                                            primary=True, verified=True)
        social_user = SocialAccount.objects.create(user=user,
                                                   uid=mocked_uid,
                                                   provider=FacebookProvider.id)
        with mock.patch('allauth.socialaccount.providers.facebook.views.'
                        'requests') as requests_mock:
            requests_mock.get.return_value.json.return_value = mocked_resp.json()

            resp = self.api_client.post(url, data=data)
            self.assertValidJSONResponse(resp)
            expected_key = ApiKey.objects.get(user=user).key
            self.assertEqual(self.deserialize(resp),
                             dict(key=expected_key, username=user.username))
