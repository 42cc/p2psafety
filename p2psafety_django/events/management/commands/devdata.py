import datetime
import random

from django.core.management.base import BaseCommand
from django.contrib.auth.models import User
from django.contrib.gis import geos

from events.tests.helpers.factories import EventFactory, EventUpdateFactory
from users.tests.helpers import UserFactory
from users.models import Role, MovementType


class Command(BaseCommand):
    """
    Creates test database records for local development.
    """
    help = __doc__

    def setup(self):
        data = {
            'main_user': User.objects.get_or_create(username="devdata_user")[0],
            'supporter_user': User.objects.get_or_create(username="supporter_user")[0],
            'another_user': User.objects.get_or_create(username="another_user")[0],
            'roles': [Role.objects.get_or_create(name="activist")[0],
                      Role.objects.get_or_create(name="journalist")[0]],
            'movement_types': [MovementType.objects.get_or_create(name="feet")[0],
                              MovementType.objects.get_or_create(name="car")[0]],
            'timestamp_start': datetime.datetime.now() - datetime.timedelta(days=5),
            'location_start': geos.Point(390.56, 50.43),
        }
        data['supporter_user'].roles.add(data['roles'][0])
        data['supporter_user'].roles.add(data['roles'][1])
        data['supporter_user'].movement_types.add(data['movement_types'][1])

        data['another_user'].roles.add(data['roles'][0])
        data['another_user'].movement_types.add(data['movement_types'][0])
        return data

    def create_point_generator(self, location_start):
        location_x = location_start.x
        location_y = location_start.y

        while True:
            location_x += (random.random() - 0.5) * 0.01
            location_y += (random.random() - 0.5) * 0.01
            yield geos.Point(location_x, location_y)

    def create_timestamp_generator(self, timestamp_start):
        """
        Updates every hour.
        """
        delta_hours = 0
        while True:
            delta_hours += 1
            yield timestamp_start + datetime.timedelta(hours=delta_hours)

    def create_args_generator(self, location_start=None, event=None, **kwargs):
        points = self.create_point_generator(location_start)
        t = ' for event %d' % event.id if event else ''

        # Empty update
        yield dict(audio=None, video=None)

        # Text only
        yield dict(text='Some text' + t, audio=None, video=None)

        # Location
        points_num = 4
        for i in xrange(points_num):
            yield dict(location=points.next(), audio=None, video=None,
                       text=('Location update' + t if i > points_num / 2 else ''))

        # Audio
        yield dict(video=None)
        yield dict(text='Audio record' + t, video=None)

        # Video
        yield dict(audio=None)
        yield dict(text='Video record' + t, audio=None)

        # Audio & video
        yield dict()
        yield dict(text='Audio and video data' + t)

        # Audio & video & location
        yield dict(location=points.next())
        yield dict(text='Audio, video and locatio data', location=points.next())

    def handle(self, *args, **kwargs):
        cfg = self.setup()
        event = EventFactory(user=cfg['main_user'])
        supporting_event = EventFactory(user=cfg['supporter_user'], type=1)
        supporting_event.supported.add(event)

        another_event = EventFactory(user=cfg['another_user'], type=1)
        another_event.supported.add(event)

        created = []

        args_generator = self.create_args_generator(event=event, **cfg)
        timestamp_generator = self.create_timestamp_generator(cfg['timestamp_start'])
        for entity_args in args_generator:
            created.append(EventUpdateFactory(event=event,
                                              timestamp=timestamp_generator.next(),
                                              **entity_args))

        args_generator = self.create_args_generator(event=supporting_event, **cfg)
        timestamp_generator = self.create_timestamp_generator(cfg['timestamp_start'])
        for entity_args in args_generator:
            created.append(EventUpdateFactory(event=supporting_event,
                                              timestamp=timestamp_generator.next(),
                                              **entity_args))

        args_generator = self.create_args_generator(event=another_event, **cfg)
        timestamp_generator = self.create_timestamp_generator(cfg['timestamp_start'])
        for entity_args in args_generator:
            created.append(EventUpdateFactory(event=another_event,
                                              timestamp=timestamp_generator.next(),
                                              **entity_args))

        print 'Created %d event updates' % len(created)
