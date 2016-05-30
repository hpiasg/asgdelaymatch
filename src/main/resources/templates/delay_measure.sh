#!/bin/sh

dc_shell -f "#*dc_tcl*#" > /dev/null
status=$?
if [ "$status" == "2" ] ; then
	echo success
elif [ "$status" == "1" ] ; then
	echo analyze error
fi
