import logging

from django.test.runner import DiscoverRunner


class TestRunner(DiscoverRunner):

    def run_tests(self, test_labels, extra_tests=None, **kwargs):
        logging.disable(logging.INFO)
        return super(TestRunner, self).run_tests(test_labels, extra_tests, **kwargs)
