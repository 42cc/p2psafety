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

from .helpers import ModelsMixin, UserFactory, RoleFactory, SocialTestCase


class PermissionTestCase(ModelsMixin, ResourceTestCase):

    def test_get_users_list(self):
        """
        Noone can access user list.
        """
        self.assertHttpMethodNotAllowed(self.api_client.get(self.users_list_url))

    def test_get_users_detail(self):
        """
        Noone can view user details.
        """
        user = UserFactory()
        self.assertHttpMethodNotAllowed(self.api_client.get(self.users_detail_url(user.id)))

    def test_get_list_roles(self):
        """
        Anyone can access roles list.
        """
        self.assertHttpOK(self.api_client.get(self.roles_list_url, format='json'))

    def test_role_add_remove(self):
        """
        Anyone can add/remove role.
        """
        url = self.users_roles_url(1)
        self.assertNotEqual(self.api_client.post(url).status_code, 403)
        self.assertNotEqual(self.api_client.delete(url).status_code, 403)


class RolesTestCase(ModelsMixin, ResourceTestCase):

    def test_get_list(self):
        role1, role2 = RoleFactory(), RoleFactory()

        resp = self.api_client.get(self.roles_list_url, format='json')
        self.assertValidJSONResponse(resp)
        roles_dicts = sorted(self.deserialize(resp)['objects'], key=itemgetter('id'))
        self.assertEqual(dict(id=role1.id, name=role1.name), roles_dicts[0])
        self.assertEqual(dict(id=role2.id, name=role2.name), roles_dicts[1])


class UsersTestCase(ModelsMixin, ResourceTestCase):

    def test_get_roles(self):
        user = UserFactory()
        role1, role2 = RoleFactory(), RoleFactory()
        user.roles.add(role1)
        url = self.users_roles_url(user.id)

        resp = self.api_client.get(url, format='json')
        self.assertValidJSONResponse(resp)
        roles_list = self.deserialize(resp)
        self.assertEqual(roles_list, [role1.id])

    def test_set_roles(self):
        # User has 1100
        user = UserFactory()
        role0, role1, role2, role3 = RoleFactory(), RoleFactory(), RoleFactory(), RoleFactory()
        user.roles.add(role0, role1)
        url = self.users_roles_url(user.id)
        
        # Setting 0110
        data = {'role_ids': [role1.id, role2.id]}
        
        # Results in 0110
        resp = self.api_client.post(url, data=data)
        self.assertEqual(resp.status_code, 202)
        self.assertEqual(data['role_ids'], [r.id for r in user.roles.all()])

    def test_set_single_role(self):
        user, role = UserFactory(), RoleFactory()
        url = self.users_roles_url(user.id)

        resp = self.api_client.post(url, data=dict(role_ids=role.id))
        self.assertEqual(resp.status_code, 202)
        self.assertEqual(list(user.roles.all()), [role])

    def test_clear_roles(self):
        user, role = UserFactory(), RoleFactory()
        url = self.users_roles_url(user.id)
        user.roles.add(role)

        resp = self.api_client.post(url, data=dict(role_ids=[]))
        self.assertEqual(resp.status_code, 202)
        self.assertEqual(user.roles.count(), 0)

    def test_role_errors(self):
        user = UserFactory()
        role = RoleFactory()
        existing_user = self.users_roles_url(user.id)
        not_existing_user = self.users_roles_url(user.id + 1)
        
        # User does not exist
        data = dict(role_ids=[])
        self.assertHttpNotFound(self.api_client.post(not_existing_user, data=data))

        # No ``role_ids`` supplied
        resp = self.api_client.post(existing_user, data={})
        self.assertEqual(resp.status_code, 400)

        # Invalid body
        resp = self.api_client.post(existing_user, data='invalid data')
        self.assertEqual(resp.status_code, 400)      


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
