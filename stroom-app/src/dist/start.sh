#!/usr/bin/env bash
#
# Starts Stroom

# shellcheck disable=SC1091
source bin/utils.sh
source config/scripts.env

start() {
  # We pipe the output of nohup to /dev/null because everything we might need for debugging
  # should be caputured somewhere in 'logs'. If there's a problem starting and the logs aren't
  # helping then try removing ' > /dev/null 2>&1' from the end and re-running.
  # We need word splitting so we need to disable SC2086
  # shellcheck disable=SC2086
  nohup java ${JAVA_OPTS} -jar "${PATH_TO_JAR}" server "${PATH_TO_CONFIG}" &> logs/start.sh.log&

  echo $! > "${PID_FILE}"

  echo
  info "Waiting for stroom to complete its start up."
  info "Stroom has to build its database tables when started for the first time, so this may take a minute or so. Subsequent starts will be quicker."

  wait_for_200_response "http://localhost:${STROOM_ADMIN_PORT}/admin"

  # Stroom is now up or we have given up waiting so check the health
  ./health.sh

  # Display the banner, URLs and login details
  ./info.sh
}

main(){
  check_is_configured

  if [ ! -f "${PID_FILE}" ]; then # If there is no pid file
    start
  else # If there is a pid file we need to deal with it
    local -r PID=$(cat "$PID_FILE");

    if [ "$PID" = '' ]; then # If the pid file is empty for some reason
      start
    else 
      if ps -p "$PID" > /dev/null # Check if the PID is a running process
      then
        warn "Stroom is already running (pid: ${BLUE}$PID${NC}). Use ${BLUE}restart.sh${NC} if you want to start it."
        # echo -e "${RED}Warning:${NC} ${GREEN}Stroom${NC} is already running (pid: ${BLUE}$PID${NC}). Use ${BLUE}restart.sh${NC} if you want to start it."
      else 
        warn "There was an instance of Stroom running but it looks like it wasn't stopped gracefully. You might want to check the logs."

        read -n1 -r -p " - Would you like to start a new instance? (y/n)" start_new_instance
        echo -e ''
        if [ "$start_new_instance" = 'y' ]; then
          rm "$PID_FILE"
          start
        else 
          info "Ok. I won't start anything."
        fi
      fi
    fi
  fi
}

main "$@"
