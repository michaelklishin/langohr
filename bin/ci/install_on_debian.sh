#!/bin/sh

sudo apt-key adv --keyserver "hkps.pool.sks-keyservers.net" --recv-keys "0x6B73A36E6026DFCA"

sudo tee /etc/apt/sources.list.d/bintray.rabbitmq.list <<EOF
deb https://dl.bintray.com/rabbitmq-erlang/debian xenial erlang
deb https://dl.bintray.com/rabbitmq/debian xenial main
EOF

sudo apt-get update -y
sudo apt-get install -y rabbitmq-server

until lsof -i:5672; do echo "Waiting for RabbitMQ to start..."; sleep 1; done

