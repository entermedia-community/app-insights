#!/bin/bash +x
# USAGE setupresiliodrive.sh SECRET /path/to/hotfolder
if [ "$#" -ne 2 ]; then
   echo "Illegal number of parameters, restart , Config File Required"
   exit
fi
THECONFIGDIR=$2

killall rslsync
HOME=$THECONFIGDIR
export HOME
cd $HOME
sleep 5
rslsync --config $THECONFIGDIR/sync.conf --log $THECONFIGDIR/sync.log  
