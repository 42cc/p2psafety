from livesettings import ConfigurationSettings


def set_livesettings_value(group_name, value_name, value):
    """
    Gives ability to Set livesettings config values from code.
    """
    mgr = ConfigurationSettings()
    config_field = mgr.get_config(group_name, value_name)
    config_field.update(value)
