from livesettings import (
    config_register,
    ConfigurationGroup,
    PositiveIntegerValue,
    MultipleStringValue,
    StringValue,
    BooleanValue,
)
from django.utils.translation import ugettext_lazy as _

EVENTS_GROUP = ConfigurationGroup(
    'Events',               # key: internal name of the group to be created
    _('Settings for app Events'),  # name: verbose name which can be automatically translated
)

config_register(PositiveIntegerValue(
    EVENTS_GROUP,           # group: object of ConfigurationGroup created above
    'operator_wake_up_alert_interval',      # key:   internal name of the configuration value to be created
    description=_('Time for operator alert in minutes'),   # label for the value
    default=10        # value used if it have not been modified by the user interface
))
