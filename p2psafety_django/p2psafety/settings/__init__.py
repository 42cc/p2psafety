from django import *
from apps import *

try:
    from local import *
except:
    print '***p2psafety/settings/local.py not found***'
