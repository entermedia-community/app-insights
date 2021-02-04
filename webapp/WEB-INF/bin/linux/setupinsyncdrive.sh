#!/bin/bash +x
# USAGE setupinsyncdrive.sh AUTH_CODE /path/to/hotfolder
if [ "$#" -ne 3 ]; then
   echo "Illegal number of parameters, restart , Config File Required"
   exit
fi

##			List<String> com = Arrays.asList("add_account","-a", key,"-p",hotfolderpath,"-e","link");
##			ExecResult result = getExec().runExec("insync-portable",com,true);


HOMEDIR=$1
THEAUTHCODE=$2
HOTFOLDER=$3

HOME=$HOMEDIR
export HOME
cd $HOME


#Is not running, start it
ISRUNNING=$(insync-portable get_status)
RUNNING="Start it first"
if [[ ${ISRUNNING} =~ ${RUNNING} ]];then
   echo "Starting insync"
   insync-portable start
fi
sleep 2
insync-portable add_account -a $THEAUTHCODE -e link -p $HOTFOLDER
  
