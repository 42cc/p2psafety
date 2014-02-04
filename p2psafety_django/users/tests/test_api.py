from operator import itemgetter

from tastypie.test import ResourceTestCase

from .helpers import ModelsMixin, RoleFactory


class PermissionTestCase(ModelsMixin, ResourceTestCase):

    def test_get_list_roles(self):
        """
        Anyone can access roles list.
        """
        self.assertHttpOK(self.api_client.get(self.roles_list_url, format='json'))


class RolesTestCase(ModelsMixin, ResourceTestCase):

    def test_get_list(self):
        role1, role2 = RoleFactory(), RoleFactory()

        resp = self.api_client.get(self.roles_list_url, format='json')
        self.assertValidJSONResponse(resp)
        roles_dicts = sorted(self.deserialize(resp)['objects'], key=itemgetter('id'))
        self.assertDictContainsSubset(dict(id=role1.id, name=role1.name), roles_dicts[0])
        self.assertDictContainsSubset(dict(id=role2.id, name=role2.name), roles_dicts[1])
