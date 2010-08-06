#!/bin/bash
PORT=$1
source h1-node-command
if [ -e /opt/h1/h1-node.pid ]; then
  echo "H1 Server is already running. Exiting".
  exit
fi

exec $LAUNCH_COMMAND $PORT & echo $! >/opt/h1/h1-node.pid
