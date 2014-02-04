import mock

from .factories import UserFactory


def mock_get_backend(module_path='events.api.resources'):
    def decorator(func):
        def decorated(self, *args, **kwargs):
            with mock.patch(module_path + '.get_backend') as mocked_get_backend:
                self.auth_user = UserFactory()
                self.mocked_get_backend = mocked_get_backend
                mocked_get_backend()().do_auth.return_value = self.auth_user
                result = func(self, *args, **kwargs)
                del self.auth_user
            return result
        return decorated
    return decorator