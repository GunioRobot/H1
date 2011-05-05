#!/bin/bash

## This script will install a H1 Node as a service
cp /opt/h1/h1-node.init.d /etc/init.d/h1-node
chmod 755 /etc/init.d/h1-node
chown root /etc/init.d/h1-node
chgrp root /etc/init.d/h1-node
if [ -a /etc/redhat-release ]
then
 chkconfig --add h1-node
else
 update-rc.d h1-node defaults
fi
exit


