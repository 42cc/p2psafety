{% autoescape off %}

{% for key,endpoint in endpoints.items %}
{{ endpoint.list_endpoint }}
----------------------------------------------------------

{% if endpoint.docstring %}{{ endpoint.docstring }}{% endif %}

**Model Fields**:
{% for field, field_meta in endpoint.schema.fields.items %}
    ``{{ field }}``:
        :Type:
            {{ field_meta.type }}
        :Description: 
            {{ field_meta.help_text }}
        :Nullable: 
            {{ field_meta.nullable }}
        :Readonly:
            {{ field_meta.readonly }} 
{% endfor %}

JSON Response ::

    {{% for field, field_meta in endpoint.schema.fields.items %}
        {{ field }}:<{{ field_meta.type }}>,{% endfor %}
    }

{% for method_name, method_data in endpoint.extra_actions.items %}
{{ method_data.url }}
-----------------------------------------------------------

{{ method_data.description }}

**Methods**:
    {% for sub_method_data in method_data.methods %}
    :{{ sub_method_data.name|upper }}:

    {{ sub_method_data.description}}
    {% endfor %}
{% endfor %}

{% endfor %}
{% endautoescape %}
