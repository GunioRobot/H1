# h1.pp

class h1 {
                include zookeeper, java
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
                s3get { "h1/$h1Dist":
                        cwd     => "/opt/h1",
                        name    => "$h1Dist",
                        expires => "900",
                        before  => Exec["tar -zxf $h1Dist"],
                        bucket  => "talis-distros",
                        require => [ File["/opt/$h1dir"] ],
                }
        } else {
        exec { "get-h1":
                    cwd     => "/opt/h1",
                    creates => "/opt/h1/$h1Dist",
                    path    => ["/usr/bin", "/usr/sbin"],
                    before  => Exec["tar -zxf $h1Dist"],
                    command => "curl -s -f -o $h1Dist http://$repoUrl/h1/$h1Dist",
                    require => [ File["/opt/$h1dir"] ],
            }
        }
        exec { "tar -zxf $h1Dist":
                cwd     => "/opt/h1",
                creates => "/opt/h1/lib",
        onlyif  => "ls /opt/h1/$h1Dist",
                path    => "/bin";
           "./install_h1_service.sh":
        cwd         => "/opt/$h1dir",
        creates     => "/etc/init.d/h1-node",
        require     => Exec["tar -zxf $h1Dist"],
        path        => ["/bin", "/sbin"];
        }
    service { "h1-node" :
        ensure  => running,
        pattern => "h1",
        require => [ Exec["./install_h1_service.sh"], Class["java"] ],
    }
}
