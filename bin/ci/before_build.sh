#!/bin/sh

CTL=${LANGOHR_RABBITMQCTL:="sudo rabbitmqctl"}
PLUGINS=${LANGOHR_RABBITMQ_PLUGINS:="sudo rabbitmq-plugins"}

case $CTL in
        DOCKER*)
          PLUGINS="docker exec ${CTL##*:} rabbitmq-plugins"
          CTL="docker exec ${CTL##*:} rabbitmqctl";;
esac

echo "Will use rabbitmqctl at ${CTL}"
echo "Will use rabbitmq-plugins at ${PLUGINS}"

$PLUGINS enable rabbitmq_management

sleep 3

# guest:guest has full access to /

$CTL add_vhost /
$CTL add_user guest guest
$CTL set_permissions -p / guest ".*" ".*" ".*"

# Reduce retention policy for faster publishing of stats
$CTL eval 'supervisor2:terminate_child(rabbit_mgmt_sup_sup, rabbit_mgmt_sup), application:set_env(rabbitmq_management,       sample_retention_policies, [{global, [{605, 1}]}, {basic, [{605, 1}]}, {detailed, [{10, 1}]}]), rabbit_mgmt_sup_sup:start_child().'
$CTL eval 'supervisor2:terminate_child(rabbit_mgmt_agent_sup_sup, rabbit_mgmt_agent_sup), application:set_env(rabbitmq_management_agent, sample_retention_policies, [{global, [{605, 1}]}, {basic, [{605, 1}]}, {detailed, [{10, 1}]}]), rabbit_mgmt_agent_sup_sup:start_child().'

# langohr:langohr.password has full access to / and langohr_testbed

$CTL add_vhost langohr_testbed
$CTL add_user langohr "langohr.password"
$CTL set_permissions -p /               langohr ".*" ".*" ".*"
$CTL set_permissions -p langohr_testbed langohr ".*" ".*" ".*"

$CTL set_permissions -p /               guest ".*" ".*" ".*"
$CTL set_permissions -p langohr_testbed guest ".*" ".*" ".*"

sleep 3
