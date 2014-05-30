-include Makefile.def

# targets
.PHONY: manage run mailserver syncdb shell test clean migrate init_migrate loaddata help

manage:
ifndef CMD
	@echo Please, specify -e CMD=command argument to execute
else
	$(MANAGE) $(CMD)
endif

static:
	@echo collecting static
	$(MANAGE) collectstatic --noinput

run:
	@echo Starting $(PROJECT_NAME) ...
	$(MANAGE) runserver $(BIND_TO):$(BIND_PORT)

mailserver:
	python -m smtpd -n -c DebuggingServer $(BIND_TO):$(MAILSERVER_PORT)

celery:
	celery -A p2psafety_django.p2psafety worker -l info

syncdb:
	@echo Syncing...
	$(MANAGE) syncdb --noinput
	$(MANAGE) migrate
	@echo Done

shell:
	@echo Starting shell...
	$(MANAGE) shell

test:
	#TESTING=1 $(TEST) --noinput $(TEST_OPTIONS) && \
	cd p2psafety-android/p2psafety && make all

coverage:
	TESTING=1 coverage run --source='.' p2psafety_django/manage.py test
	coverage report -m

testone:
	$(TEST) $(filter-out $@,$(MAKECMDGOALS))

clean:
	@echo Cleaning up...
	find . -name "*.pyc" -exec rm -rf {} \;
	@echo Done

migrate:
ifndef APP_NAME
	@echo You can also specify -e APP_NAME=some_app
	$(MANAGE) migrate
else
	@echo Starting of migration of $(APP_NAME)
	$(MANAGE) schemamigration $(APP_NAME) --auto
	$(MANAGE) migrate $(APP_NAME)
	@echo Done
endif

init_migrate:
ifndef APP_NAME
	@echo Please, specify -e APP_NAME=appname argument
else
	@echo Starting init migration of $(APP_NAME)
	$(MANAGE) schemamigration $(APP_NAME) --initial
	$(MANAGE) migrate $(APP_NAME)
	@echo Done
endif

loaddata:
	@echo Load data from fixtures ...
	$(MANAGE) loaddata $(FIXTURES)

makemessages:
	@echo Preparing .po files ...
	cd p2psafety_django && python manage.py makemessages --all

compilemessages:
	@echo Preparing .mo files ...
	$(MANAGE) compilemessages

help:
	@cat INSTALL.rst
