# Vagrant-Puppet-Django

This is a Vagrant Ubuntu (Saucy 64) box with a single Puppet manifests file (no modules) for Django development.
Based on https://github.com/meako689/vagrant-puppet-django

## What's included

Puppet provisions:

- Nginx
- uWSGI
- Postgis
- Python
- Virtualenv
- Django
- PIL dependencies to support jpg, zlib, freetype
- Git
- Vim

## Usage

- Edit configuration variables in the `p2psafety.pp` file
- `vagrant up`

That's it! You are all set.

## Note

You may want to:

- Run `git config --global` to set your `user.name` and `user.email`
- Edit `my.cnf` file to configure charset and collation to UTF8 as follows:

```ini
[client]
default-character-set = utf8

[mysqld]
character-set-server = utf8
collation-server = utf8_general_ci
```
