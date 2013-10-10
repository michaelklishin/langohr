#!/bin/sh

${RABBITMQCTL:="sudo rabbitmqctl"}
${RABBITMQ_PLUGINS:="sudo rabbitmq-plugins"}
${RABBITMQ_SERVER:="sudo rabbitmq-server"}

# guest:guest has full access to /

$RABBITMQCTL add_vhost /
$RABBITMQCTL add_user guest guest
$RABBITMQCTL set_permissions -p / guest ".*" ".*" ".*"


# langohr:langohr.password has full access to / and langohr_testbed

$RABBITMQCTL add_vhost langohr_testbed
$RABBITMQCTL add_user langohr "langohr.password"
$RABBITMQCTL set_permissions -p /               langohr ".*" ".*" ".*"
$RABBITMQCTL set_permissions -p langohr_testbed langohr ".*" ".*" ".*"

$RABBITMQCTL set_permissions -p /               guest ".*" ".*" ".*"
$RABBITMQCTL set_permissions -p langohr_testbed guest ".*" ".*" ".*"
$RABBITMQ_PLUGINS enable rabbitmq_federation
# need to restart RabbitMQ after changing the plugin configuration
$RABBITMQCTL stop
$RABBITMQ_SERVER restart
sleep 3
