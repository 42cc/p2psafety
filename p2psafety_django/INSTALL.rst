Installation instructions
=========================

TBD.

Local installation
==================

Create virtualenv as usual. Then run::
  pip install -r requirements/dev.txt
  cp Makefile.def.default Makefile.def
  cp p2psafety_django/p2psafety/settings/local.py.default p2psafety_django/p2psafety/settings/local.py

Install PostGIS as described here:
  https://docs.djangoproject.com/en/dev/ref/contrib/gis/install/postgis/

Then you can run app locally by
  make run


    You can use "events.devdata" command to generate some Event & EventUpdate
records for testing purposes::
    python manage.py devdata

Warning! Do not user this type of installation on production, change your
SECRET_KEY in settings/local.py before deploying to production. Keep your secrets
to yourself. Have fun.