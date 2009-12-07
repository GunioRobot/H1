# h1.pp

class h1 {
		include zookeeper
        $h1dir = "h1"
    group { "h1":
                name   => "h1",
                before => User["h1"],
                ensure => present,
    }
    user { "h1":
                gid    => h1,
                ensure => present,
    }
    file { "/opt/$h1dir":
                ensure => directory,
                owner  => h1,
                group  => h1;
    }
        if $s3 == "true" {
                s3get { "h1/$h1dist":
                        cwd     => "/opt/h1",
                        name    => "$h1dist",
                        expires => "900",
                        before  => Exec["tar -zxf $h1dist"],
                        bucket  => "talis-distros",
                        require => [ File["/opt/$h1dir"] ],
                }
        } else {
        exec { "get-h1":
                    cwd     => "/opt/h1",
                    creates => "/opt/h1/$h1dist",
                    path    => ["/usr/bin", "/usr/sbin"],
                    before  => Exec["tar -zxf $h1dist"],
                    command => "curl -s -f -o $h1dist http://$repoUrl/h1/$h1dist",
                    require => [ File["/opt/$h1dir"] ],
            }
        }
        exec { "tar -zxf $h1dist":
                cwd     => "/opt/h1",
                creates => "/opt/h1/lib",
        onlyif  => "ls /opt/$h1dist",
                path    => "/bin";
           "./install_h1_service.sh":
        cwd         => "/opt/$h1dir",
        creates     => "/etc/init.d/h1-node",
        require     => Exec["tar -zxf $h1dist"],
        path        => ["/bin"];
        }
    service { "h1" :
        ensure  => running,
        pattern => "h1",
        require => [ File["/etc/init.d/h1-node"] ],
    }
}