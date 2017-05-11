#!/usr/bin/env bash

#run with
# docker run --expose=4444 ...

source ${APP_HOME}/.noderc

if [ -z "$HUB_PORT_4444_TCP_ADDR" ]; then
  echo Not linked with a running Hub container 1>&2
  exit 1
fi

: ${NODE_MAX_INSTANCES:=1} # As integer, maps to "maxInstances"
: ${NODE_MAX_SESSION:=1} # As integer, maps to "maxSession"
: ${NODE_PORT:=5555} # As integer, maps to "port"
: ${NODE_REGISTER_CYCLE:=5000} # In milliseconds, maps to "registerCycle"
: ${NODE_POLLING:=5000} # In milliseconds, maps to "nodePolling"
: ${NODE_UNREGISTER_IF_STILL_DOWN_AFTER:=60000} # In milliseconds, maps to "unregisterIfStillDownAfter"
: ${NODE_DOWN_POLLING_LIMIT:=2} # As integer, maps to "downPollingLimit"
: ${NODE_APPLICATION_NAME:=""} # As string, maps to "applicationName"
: ${NODE_NDEBUG:=false} # As string, maps to "applicationName"

CONF=${HOME}/config_node.json

cat > ${CONF} <<_EOF
{
    "capabilities": [ {
        "version": "${NODE_BROWSER_VERSION}",
        "browserName": "${NODE_BROWSER_NAME}",
        "maxInstances": ${NODE_MAX_INSTANCES},
        "seleniumProtocol": "WebDriver",
        "extension.bravoCapability": true,
        "applicationName": "${NODE_APPLICATION_NAME}"
    }],
    "proxy": "org.openqa.grid.selenium.proxy.DefaultRemoteProxy",
    "maxSession": ${NODE_MAX_SESSION},
    "port": ${NODE_PORT},
    "register": true,
    "registerCycle": ${NODE_REGISTER_CYCLE},
    "servlets": [
        "com.bravostudiodev.grid.BravoExtensionServlet"
    ],
    "nodePolling": ${NODE_POLLING},
    "unregisterIfStillDownAfter": ${NODE_UNREGISTER_IF_STILL_DOWN_AFTER},
    "downPollingLimit": ${NODE_DOWN_POLLING_LIMIT}
}
_EOF

echo "starting node with configuration:"
cat ${CONF}
echo "SE_OPTS=${SE_OPTS}"

function shutdownhandler {
  echo "shutting down node.."
  kill -s SIGTERM ${NODE_PID}
  /etc/init.d/dbus stop
  wait ${NODE_PID}
  echo "shutdown complete"
}

#set -x
#HUB_PORT_4444_TCP_ADDR=172.18.0.2 HUB_PORT_4444_TCP_PORT=4444 NODE_NDEBUG=true ./entry_node.sh
#xinit /usr/bin/java -cp "/home/app/selenium/*" org.openqa.grid.selenium.GridLauncherV3 -role node -hub http://172.18.0.2:4444/grid/register -nodeConfig /home/app/selenium/config.json -- :100 -dpi 96 -noreset -nolisten tcp +extension GLX +extension RANDR +extension RENDER -logfile ${HOME}/XpraXorg-10.log -config ${HOME}/xorg.conf

NODE_LAUNCH_ARGS="org.openqa.grid.selenium.GridLauncherV3 \
  -role node \
  -hub http://${HUB_PORT_4444_TCP_ADDR}:${HUB_PORT_4444_TCP_PORT}/grid/register \
  -nodeConfig ${CONF} \
  ${SE_OPTS}"

rm -f /tmp/.X*lock
sed -e "s|Virtual 1360 1020|Virtual ${SCREEN_WIDTH} ${SCREEN_HEIGHT}|" -i "${HOME}/xorg.conf"
XORG_ARGS="-dpi 96 -noreset -nolisten tcp +extension GLX +extension RANDR +extension RENDER -logfile ${HOME}/XpraXorg-10.log -config ${HOME}/xorg.conf"

set -x

cat > "$HOME/fluxbox-startup" <<_EOF
#!/bin/sh
/usr/bin/java ${JAVA_OPTS} -cp "${HOME}/selenium/*" ${NODE_LAUNCH_ARGS} &
exec fluxbox #-log ".fluxbox/log"
_EOF
chmod +x "$HOME/fluxbox-startup"

/etc/init.d/dbus restart

if ! ${NODE_NDEBUG}; then
  XPRA_INITENV_COMMAND="xpra initenv" xpra --no-daemon --no-mdns --no-pulseaudio --xvfb="Xorg ${XORG_ARGS}" \
    start-desktop ${DISPLAY} --exit-with-child --start-child="${HOME}/fluxbox-startup" --bind-tcp=0.0.0.0:10000 &
  NODE_PID=$!
else
  # xinit client excutable path must start with / or .
  xinit ${HOME}/fluxbox-startup -- ${DISPLAY} ${XORG_ARGS} &
  NODE_PID=$!
fi

trap shutdownhandler SIGTERM SIGINT
wait ${NODE_PID}
/etc/init.d/dbus stop
