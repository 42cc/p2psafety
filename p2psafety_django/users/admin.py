from django.contrib import admin

from . import models


class ProfileAdmin(admin.ModelAdmin):
    list_display = ('user', 'phone_number')


class RoleAdmin(admin.ModelAdmin):
    fields = ('name',)
    list_display = ('id', 'name')
        

class MovementTypeAdmin(admin.ModelAdmin):
    fields = ('name',)
    list_display = ('id', 'name')


admin.site.register(models.Profile, ProfileAdmin)
admin.site.register(models.Role, RoleAdmin)
admin.site.register(models.MovementType, MovementTypeAdmin)
