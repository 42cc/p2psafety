from tastypie.models import ApiKey


def get_api_token(user):
    try:
        return ApiKey.objects.filter(user=user)[0]
    except IndexError:
        return ApiKey.objects.create(user=user)
