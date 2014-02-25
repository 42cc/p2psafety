from django.contrib.auth import logout
from django.shortcuts import redirect


def log_the_fuck_out(request):
    user = request.user
    user.is_active = False
    user.password = ""
    user.save()
    logout(request)
    return redirect('/')
