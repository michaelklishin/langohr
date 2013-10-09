#!/bin/sh

${RABBITMQCTL:="sudo rabbitmqctl"}
${RABBITMQ_PLUGINS:="sudo rabbitmq-plugins"}

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
sleep 3
