#!/bin/sh

git format-patch -n HEAD^ --stdout | curl -X POST \
    http://localhost:8000/commit-msg -H 'Content-Type: text/plain' -d @-

