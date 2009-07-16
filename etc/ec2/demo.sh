#!/bin/bash
sudo cp proxy.serverlist /tmp
sudo cp demoNodes.js /var/www/monitor-app
sudo cp .seedlist /opt/majat/SeedList
sudo cp Seed* /opt/majat/

chmod +x css*.sh

cat gmetad.conf.template1 > gmetad.conf
cat ganglia-partial.conf >> gmetad.conf
cat gmetad.conf.template2 >> gmetad.conf
scp gmetad.conf puppetdrop@punch.kc.talis.local:/var/www/html/chroot/puppetdrop/templates
