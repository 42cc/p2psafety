from django.contrib.auth import logout
from django.shortcuts import redirect
from django.contrib.auth.decorators import login_required


@login_required
def log_the_fuck_out(request):
    user = request.user
    user.is_active = False
    user.password = ""
    user.save()
    logout(request)
    return redirect('/')
