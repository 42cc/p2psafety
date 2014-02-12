from operator import itemgetter

from django.contrib.auth.models import User

from tastypie.test import ResourceTestCase

from .helpers import ModelsMixin, UserFactory, RoleFactory


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
        data = {'role_id': [role1.id, role2.id]}
        
        # Results in 0110
        resp = self.api_client.post(url, data=data)
        self.assertEqual(resp.status_code, 202)
        self.assertEqual(data['role_id'], [r.id for r in user.roles.all()])

    def test_role_errors(self):
        user = UserFactory()
        role = RoleFactory()
        existing_user = self.users_roles_url(user.id)
        not_existing_user = self.users_roles_url(user.id + 1)

        # User does not exist
        self.assertHttpNotFound(self.api_client.post(not_existing_user))

        # No ``role_id`` supplied
        resp = self.api_client.post(existing_user, data={})
        self.assertEqual(resp.status_code, 400)

        # Invalid body
        resp = self.api_client.post(existing_user, data='invalid data')
        self.assertEqual(resp.status_code, 400)

        # Invalid ``role_id`` param
        resp = self.api_client.post(existing_user, data={'role_id': '1'})
        self.assertEqual(resp.status_code, 400)        
