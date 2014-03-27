Exec { path => '/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin' }

# Global variables
$inc_file_path = '/vagrant/p2psafety_vagrant/files' # Absolute path to the files directory (If you're using vagrant, you can leave it alone.)
$tz = 'Europe/Kiev' # Timezone
$project = 'p2psafety' # Used in nginx and uwsgi
$django_path = "${project}/p2psafety_django"

$db_name = 'p2psafety' # Mysql database name to create
$db_user = 'vagrant' # Mysql username to create
$db_password = 'vagrant' # Mysql password for $db_user

include swapfile
include timezone
include user
include apt
include nginx
include uwsgi
include postgis
include python
include virtualenv
include rabbitmq
include pildeps
include software
include locale

class timezone {
  package { "tzdata":
    ensure => latest,
    require => Class['apt']
  }

  file { "/etc/localtime":
    require => Package["tzdata"],
    source => "file:///usr/share/zoneinfo/${tz}",
  }
}

class swapfile {
    #when we have not enough mem for copilation, we do swap
  exec { "bash ${inc_file_path}/bash/swapfile.sh":
    unless => 'grep -q "swapfile" /etc/fstab',
    before => Class['python']
  }
}

class user {
  # Prepare user's project directories
  file { ["/home/vagrant/virtualenvs",
          "/home/vagrant/${project}",
          "/home/vagrant/${django_path}",
          "/home/vagrant/${django_path}/static"
          ]:
    ensure => directory,
    owner => 'vagrant',
    before => File['media dir']
  }

  file { 'media dir':
    path => "/home/vagrant/${django_path}/static_media",
    ensure => directory,
    mode => 0777,
  }
}

class apt {
  package { 'python-software-properties':
    ensure => latest,
  }

  exec { 'add-apt-repository ppa:nginx/stable':
    require => Package['python-software-properties'],
    before => Exec['last ppa'],
    unless => 'ls /etc/apt/sources.list.d/ | grep nginx-stable-saucy'
  }

  exec { 'add-apt-repository ppa:ubuntugis/ubuntugis-unstable':
    before => Exec['last ppa'],
    unless => 'ls /etc/apt/sources.list.d/ | grep ubuntugis-unstable'
  }

  exec { 'last ppa':
    command => 'add-apt-repository ppa:git-core/ppa',
    require => Package['python-software-properties'],
    unless => 'ls /etc/apt/sources.list.d/ | grep git-core'
  }

  exec { 'apt-get update ':
    command => 'apt-get update',
    timeout => 0,
    require => Exec['last ppa']
  }
}

class nginx {
  package { 'nginx':
    ensure => latest,
    require => Class['apt']
  }

  service { 'nginx':
    ensure => running,
    enable => true,
    require => Package['nginx'],
    subscribe => File['sites-available config']
  }

  file { '/etc/nginx/sites-enabled/default':
    ensure => absent,
    require => Package['nginx']
  }

  file { 'sites-available config':
    path => "/etc/nginx/sites-available/${project}",
    ensure => file,
    content => template("${inc_file_path}/nginx/nginx.conf.erb"),
    require => Package['nginx']
  }

  file { "/etc/nginx/sites-enabled/${project}":
    ensure => link,
    target => "/etc/nginx/sites-available/${project}",
    require => File['sites-available config'],
    notify => Service['nginx']
  }
}

class uwsgi {
  $uwsgi_user = 'www-data'
  $uwsgi_group = 'www-data'

  package { 'uwsgi':
    ensure => latest,
    provider => pip,
    require => Class['python']
  }

  service { 'uwsgi':
    ensure => running,
    enable => true,
    require => File['apps-enabled config']
  }

  # Prepare directories
  file { ['/var/log/uwsgi', '/etc/uwsgi', '/etc/uwsgi/apps-available', '/etc/uwsgi/apps-enabled']:
    ensure => directory,
    require => Package['uwsgi'],
    before => File['apps-available config']
  }

  # Upstart file
  file { '/etc/init/uwsgi.conf':
    ensure => file,
    source => "${inc_file_path}/uwsgi/uwsgi.conf",
    require => Package['uwsgi']
  }

  # Vassals ini file
  file { 'apps-available config':
    path => "/etc/uwsgi/apps-available/${project}.ini",
    ensure => file,
    content => template("${inc_file_path}/uwsgi/uwsgi.ini.erb")
  }

  file { 'apps-enabled config':
    path => "/etc/uwsgi/apps-enabled/${project}.ini",
    ensure => link,
    target => "/etc/uwsgi/apps-available/${project}.ini",
    require => File['apps-available config']
  }
}

class postgis {
  $db_command = "psql -d template1 -U postgres -c"
  #$create_db_cmd = "CREATE DATABASE ${db_name} WITH ENCODING 'UTF8';"
  $create_db_cmd = "CREATE DATABASE ${db_name};"
  $create_user_cmd = "CREATE USER ${db_user} WITH PASSWORD '${db_password}';"
  $grant_db_cmd = "ALTER ROLE ${db_user} SUPERUSER;"
  
  $geo_cmd = "CREATE EXTENSION postgis; CREATE EXTENSION postgis_topology;"

  package { 'postgresql-9.1-postgis-2.1':
    ensure => latest,
    require => Class['apt']
  }

  package { 'postgresql-contrib':
    ensure => latest,
    require => Class['apt']
  }

  package { 'postgresql-server-dev-9.1':
    ensure => latest,
    require => Class['apt']
  }

  service { 'postgresql':
    ensure => running,
    enable => true,
    require => Package['postgresql-9.1-postgis-2.1']
  }

  exec { 'createdb':
    command => "${db_command} \"${create_db_cmd}\" ",
    user => "postgres",
    unless => "psql ${db_name} -c \"\\d\"",
    before => Exec['geocmd','dbuser'],
    require => [Class['locale'],Service['postgresql']]
  }

  exec {'dbuser':
    command => "${db_command} \"${create_user_cmd}${grant_db_cmd}\"",
    user => "postgres",
    unless => "psql ${db_name} -c \"\\du\" | grep -c ${db_user}" ,
    require => Service['postgresql']
  }

  exec {'geocmd':
    command => "psql -d ${db_name} -c \"${geo_cmd}\"",
    user => "postgres",
    unless => "psql ${db_name} -c \"select count(*) from spatial_ref_sys\"",
    require => Service['postgresql']
  }

}

class rabbitmq{
  package { 'rabbitmq-server':
    ensure => latest,
    require => Class['apt']
  }
  service { 'rabbitmq-server':
    ensure => running,
    enable => true,
    require => Package['rabbitmq-server']
  }

}

class python {
  package { 'curl':
    ensure => latest,
    require => Class['apt']
  }

  package { 'python':
    ensure => latest,
    require => Class['apt']
  }

  package { 'python-dev':
    ensure => latest,
    require => Class['apt']
  }

  package { ['libssl-dev','libxml2-dev', 'libxslt-dev']:
    ensure => latest,
    require => Class['apt']
  }


  exec { 'install-distribute':
    command => 'curl http://python-distribute.org/distribute_setup.py | python',
    require => Package['python', 'curl'],
    unless => 'which easy_install'
  }

  exec { 'install-pip':
    command => 'curl https://raw.github.com/pypa/pip/master/contrib/get-pip.py | python',
    require => Exec['install-distribute'],
    unless => 'which pip'
  }
}

class virtualenv {
  package { 'virtualenv':
    ensure => latest,
    provider => pip,
    require => Class['python', 'user']
  }

  exec { 'create virtualenv':
    command => "virtualenv ${project}",
    cwd => "/home/vagrant/virtualenvs",
    user => 'vagrant',
    creates => "/home/vagrant/virtualenvs/${project}",
    require => Package['virtualenv']
  }

  exec {'install requirements':
    command => "/home/vagrant/virtualenvs/${project}/bin/pip install -r requirements.txt",
    cwd => "/home/vagrant/${django_path}",
    user => 'vagrant',
    require => Exec['create virtualenv']
  }

  file {"/home/vagrant/.bashrc":
    ensure => file,
    mode => 0644,
    content => template("${inc_file_path}/bash/bashrc.erb"),
    require => Exec['create virtualenv']
  }
}

class pildeps {
  package { ['python-imaging', 'libjpeg-dev', 'libfreetype6-dev', 'fontconfig']:
    ensure => latest,
    require => Class['apt'],
    before => Exec['pil png', 'pil jpg', 'pil freetype']
  }

  exec { 'pil png':
    command => 'sudo ln -s /usr/lib/`uname -i`-linux-gnu/libz.so /usr/lib/',
    unless => 'test -L /usr/lib/libz.so'
  }

  exec { 'pil jpg':
    command => 'sudo ln -s /usr/lib/`uname -i`-linux-gnu/libjpeg.so /usr/lib/',
    unless => 'test -L /usr/lib/libjpeg.so'
  }

  exec { 'pil freetype':
    command => 'sudo ln -s /usr/lib/`uname -i`-linux-gnu/libfreetype.so /usr/lib/',
    unless => 'test -L /usr/lib/libfreetype.so'
  }
}

class software {
  package { 'git':
    ensure => latest,
    require => Class['apt']
  }

  package { 'mercurial':
    ensure => latest,
    require => Class['apt']
  }

  package { 'vim':
    ensure => latest,
    require => Class['apt']
  }
}

class locale{
    package{ "locales":
        ensure => latest,
    }
    file { "/var/lib/locales/supported.d/local":
        content=> "en_US.UTF-8 UTF-8\nen_GB.UTF-8 UTF-8\nuk_UA.UTF-8 UTF-8\npl.UTF-8 UTF-8",
        owner => "root",
        group => "root",
        mode => 644,
        require => Package[locales],
    }
    exec { "/usr/sbin/locale-gen":
        subscribe => File["/var/lib/locales/supported.d/local"],
        refreshonly => true,
        require => [Package[locales],File["/var/lib/locales/supported.d/local"]],
    }
}
