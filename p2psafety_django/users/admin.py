from django.contrib import admin

from models import Role, MovementType

admin.site.register(Role, admin.ModelAdmin)
admin.site.register(MovementType, admin.ModelAdmin)
