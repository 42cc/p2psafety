# Vagrant-Puppet-Django

This is a Vagrant Ubuntu (Saucy 64) box with a single Puppet manifests file (no modules) for Django development tuned to fit p2psafety project.

Based on https://github.com/meako689/vagrant-puppet-django

If yo're not familiar with vagrant, take look at [vagrant website](http://www.vagrantup.com/), and follow instructions to install.

The main concept behind it is having a virtual machine preconfigured and set up for each project. Safe, easy to use, great timesaver.

Later you may also refer to [Puppet](http://puppetlabs.com/)


## What's included

Puppet provisions:

- Nginx
- uWSGI
- Postgis
- Ejabberd
- Python
- Virtualenv
- Django
- PIL dependencies to support jpg, zlib, freetype
- Git
- Vim

and various tweaks

## Usage

in project folder:
- `vagrant up`
- grab some coffee (first build takes a long)
- `vagrant ssh`

That's it! You are all set. Now do syncdb, edit code and have fun.

*p2psafety folder in VM is synced with local folder on your computer. So you can develop editing local files.*

###runserver
in VM:

- `cd p2psafety/p2psafety_django`
- `python manage.py runserver 0.0.0.0:8000`

and it will be visible on your machine: http://127.0.0.1:8000

Also you have nginx+uwsgi set up for project, binded to 127.0.0.1:8080


## Note

You may want to:

- Run `git config --global` to set your `user.name` and `user.email`
- [Set up](http://www.jetbrains.com/pycharm/quickstart/configuring_for_vm.html) your favorite pycharm to work with vagrant
