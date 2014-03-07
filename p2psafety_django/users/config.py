from livesettings import config_register, ConfigurationGroup, values


USERS_GROUP = ConfigurationGroup('Users', 'Settings for user app')

config_register(values.BooleanValue(
    USERS_GROUP,
    'user-moderation',
    description='If flag is active, newly created users will have \"is_active\" set to False',
    default=False
))
