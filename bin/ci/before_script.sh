#!/bin/sh

${LANGOHR_RABBITMQCTL:="sudo rabbitmqctl"}
${LANGOHR_RABBITMQ_PLUGINS:="sudo rabbitmq-plugins"}

# guest:guest has full access to /

$LANGOHR_RABBITMQCTL add_vhost /
$LANGOHR_RABBITMQCTL add_user guest guest
$LANGOHR_RABBITMQCTL set_permissions -p / guest ".*" ".*" ".*"


# langohr:langohr.password has full access to / and langohr_testbed

$LANGOHR_RABBITMQCTL add_vhost langohr_testbed
$LANGOHR_RABBITMQCTL add_user langohr "langohr.password"
$LANGOHR_RABBITMQCTL set_permissions -p /               langohr ".*" ".*" ".*"
$LANGOHR_RABBITMQCTL set_permissions -p langohr_testbed langohr ".*" ".*" ".*"

$LANGOHR_RABBITMQCTL set_permissions -p /               guest ".*" ".*" ".*"
$LANGOHR_RABBITMQCTL set_permissions -p langohr_testbed guest ".*" ".*" ".*"

$LANGOHR_RABBITMQ_PLUGINS enable rabbitmq_management

sleep 3
