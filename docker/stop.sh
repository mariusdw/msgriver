#!/bin/bash

docker-compose stop

ps -ef | grep msgriver | awk {'print $2'} | head -n 1 | xargs kill
