#!/bin/bash
PORT=$1
if [ ! -n "$PORT" ] 
then
  PORT=9797
fi
source h1-node-command
if [ -e /opt/h1/h1-node.pid ]; then
  echo "H1 Server is already running. Exiting".
  exit
fi

if [ -e /tmp/user-data ]; then
  read SEED < /tmp/user-data
fi

if [ -e /tmp/my-public-address ]; then
  read EXTERNAL_ADDRESS < /tmp/my-public-address
fi

if [ -n "$SEED" ]
then
  LAUNCH_COMMAND="$LAUNCH_COMMAND -s $SEED"
fi

if [ -n "$EXTERNAL_ADDRESS" ]
then
  LAUNCH_COMMAND="$LAUNCH_COMMAND -h $EXTERNAL_ADDRESS"
fi

exec $LAUNCH_COMMAND & echo $! >/opt/h1/h1-node.pid
