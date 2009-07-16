#!/bin/bash

## This script will install a H1 Node as a service
cp h1-node.init.d /etc/init.d/h1-node
chmod 755 /etc/init.d/h1-node
chown root /etc/init.d/h1-node
chgrp root /etc/init.d/h1-node
chkconfig --add h1-node
exit


