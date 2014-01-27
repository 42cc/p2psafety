import datetime
import random

from django.core.management.base import BaseCommand, CommandError
from django.contrib.gis import geos

from events.tests.helpers import EventFactory, EventUpdateFactory, UserFactory


class Command(BaseCommand):
    """
    Creates test database records for local development.
    """
    help = __doc__

    def setup(self):
        data = {
            'main_user': UserFactory(username="devdata_user"),
            'timestamp_start': datetime.datetime.now(),
            'location_start': geos.Point(390.56, 50.43),
        }
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
        cfg = self.setup();
        event = EventFactory(user=cfg['main_user'])
        args_generator = self.create_args_generator(event=event, **cfg)
        timestamp_generator = self.create_timestamp_generator(cfg['timestamp_start'])
        created = []

        for entity_args in args_generator:
            created.append(EventUpdateFactory(event=event,
                                              timestamp=timestamp_generator.next(),
                                              **entity_args))


        print 'Created %d event updates' % len(created)
