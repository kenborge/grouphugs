#!/bin/bash
set -e
if [ -e "/tmp/gh.pid" ]
then
    kill "$(cat /tmp/gh.pid)" # kill bot
    rm /tmp/gh.pid
fi

if [[ "$1" == "stop" ]]
then
    exit
fi

./link-conf.sh
./link-db.sh

pushd "$(dirname $0)/target" # make sure we're in the correct directory
java -jar grouphug-1.0-SNAPSHOT.jar > gh.log 2>&1 & # restart bot
echo "$!" > /tmp/gh.pid # save pid
popd
