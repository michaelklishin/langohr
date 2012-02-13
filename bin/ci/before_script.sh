#!/bin/sh

# guest:guest has full access to /

sudo rabbitmqctl add_vhost /
sudo rabbitmqctl add_user guest guest
sudo rabbitmqctl set_permissions -p / guest ".*" ".*" ".*"


# langohr:langohr.password has full access to / and langohr_testbed

sudo rabbitmqctl add_vhost langohr_testbed
sudo rabbitmqctl add_user langohr "langohr.password"
sudo rabbitmqctl set_permissions -p /               langohr ".*" ".*" ".*"
sudo rabbitmqctl set_permissions -p langohr_testbed langohr ".*" ".*" ".*"
