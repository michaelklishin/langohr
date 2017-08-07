#!/bin/sh

sudo apt-get update -y
sudo apt-get install -y init-system-helpers socat adduser logrotate

cd /tmp/ || exit 1
wget https://github.com/rabbitmq/rabbitmq-server/releases/download/rabbitmq_v3_6_10/rabbitmq-server_3.6.10-1_all.deb
sudo dpkg --install rabbitmq-server_3.6.10-1_all.deb
sudo rm rabbitmq-server_3.6.10-1_all.deb

sleep 3
