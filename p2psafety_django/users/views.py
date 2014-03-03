from django.conf import settings
from django.contrib.auth import logout
from django.shortcuts import redirect
from django.contrib.auth.decorators import login_required


@login_required
def log_the_fuck_out(request):
    user = request.user
    user.is_active = False
    user.set_unusable_password()
    user.save()
    logout(request)
    return redirect(settings.ACCOUNT_LOGOUT_REDIRECT_URL)
