from django.shortcuts import render, redirect
from django.core.urlresolvers import reverse

from annoying.decorators import render_to


@render_to('site/login.html')
def login(request):
    return {'NEXT_PAGE': request.GET.get('next', '/')}