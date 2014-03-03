from tastypie.models import ApiKey


def get_api_key(user):
    """
    Use this function to get user's personal api key.

    :type user: `django.contrib.auth.models.User`
    :rtype: `tastypie.models.ApiKey`
    """
    try:
        return ApiKey.objects.filter(user=user)[0]
    except IndexError:
        return ApiKey.objects.create(user=user)
