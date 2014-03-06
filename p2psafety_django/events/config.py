from livesettings import config_register, ConfigurationGroup, values


EVENTS_GROUP = ConfigurationGroup('Events', 'Settings for events app')
EVENTS_MAP_GROUP = ConfigurationGroup('EventsMap', 'Settings for events map')

config_register(values.BooleanValue(
    EVENTS_GROUP,
    'supporters-autonotify',
    description="Should event's supporters be notified automatically on new event's updates or not",
    default=True
))

config_register(values.PositiveIntegerValue(
    EVENTS_MAP_GROUP,
    'operator-wake-up-alert-interval',
    description='Time for operator alert in minutes',
    default=7
))
config_register(values.BooleanValue(
    EVENTS_MAP_GROUP,
    'newevent-highlight',
    description='Should new events be highlighted or not',
    default=True
))
config_register(values.BooleanValue(
    EVENTS_MAP_GROUP,
    'newevent-sound',
    description='Should page play sound on new event or not',
    default=True
))

