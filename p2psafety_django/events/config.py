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
    'Events',
    _('Settings for app Events'),
)

config_register(PositiveIntegerValue(
    EVENTS_GROUP,
    'operator_wake_up_alert_interval',
    description=_('Time for operator alert in minutes'),
    default=10
))
