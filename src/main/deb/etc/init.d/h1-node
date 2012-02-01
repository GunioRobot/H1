#!/bin/sh
# chkconfig: 345 80 55
# description: H1 Server

start() {
  cd /opt/h1
  if [ -s /var/run/h1-node.pid ]; then
    read PID < /var/run/h1-node.pid
    ps -ef | grep $PID | grep -v grep >/dev/null
    RETVAL=$?
    if [ $RETVAL == 0 ]; then
      echo "H1 Server process already running, use stop or restart."
      exit 1
    fi
  fi
  exec &> /dev/null
  exec ./h1-node.sh
}

stop() {
  cd /opt/h1
  if [ ! -s /var/run/h1-node.pid ]; then
    echo "H1 Server is not running."
    exit
  fi
  kill `cat /var/run/h1-node.pid 2>/dev/null`
  rm -f /var/run/h1-node.pid
}


case $1 in

'start')
  start
  ;;
'stop')
  stop
  ;;
'restart')
  stop
  start
  ;;
*)
  echo "usage: $0 {start|stop|restart}"
  ;;
esac
