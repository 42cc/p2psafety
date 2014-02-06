from tastypie.authentication import Authentication, SessionAuthentication


class PostFreeSessionAuthentication(SessionAuthentication):
    """
    Ignores POST requests.
    """
    def is_authenticated(self, request, **kwargs):
        if request.method == 'POST':
            return True
        return super(PostFreeSessionAuthentication, self).is_authenticated(request, **kwargs)

    def get_identifier(self, request):
        if request.method == 'POST':
            return Authentication.get_identifier(self, request)
        return super(PostFreeSessionAuthentication, self).get_identifier(request)
