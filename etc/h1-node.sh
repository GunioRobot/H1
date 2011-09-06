#!/bin/bash
PORT=$1
source h1-node-command
if [ -s /var/run/h1-node.pid ]; then
  read PID < /var/run/h1-node.pid
  ps -ef | grep $PID | grep -v grep >/dev/null
  RETVAL=$?
  if [ $RETVAL == 0 ]; then
    echo "H1 Server is already running. Exiting."
    exit 1
  fi
fi

exec $LAUNCH_COMMAND $PORT & echo $! >/var/run/h1-node.pid
