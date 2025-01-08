#!/usr/bin/env bash

ARCH=`uname -m`
if [ "$ARCH" = "aarch64" ]; then
    # There is an issue with M4 arm processors, explained here:
    # https://github.com/corretto/corretto-21/issues/85
    exec java -XX:UseSVE=0 -cp "app:app/lib/*" io.tolgee.Application
else
    exec java -cp "app:app/lib/*" io.tolgee.Application
fi