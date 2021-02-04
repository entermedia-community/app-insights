#!/bin/bash +x
export CATALINA_BASE="/opt/entermediadb/tomcat"
export CATALINA_HOME="/usr/share/entermediadb/tomcat"
(sleep 2 && /opt/entermediadb/tomcat/bin/catalina.sh stop && /opt/entermediadb/tomcat/bin/catalina.sh start) &
echo "Restarting...";
