from django.contrib import admin

from models import Event, EventUpdate

admin.site.register(Event, admin.ModelAdmin)
admin.site.register(EventUpdate, admin.ModelAdmin)
