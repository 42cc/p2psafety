from django.shortcuts import render

from annoying.decorators import render_to


@render_to('events/map.html')
def map(request):
    return {}