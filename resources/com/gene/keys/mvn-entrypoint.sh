#!/bin/bash
set -o pipefail

# Copy files form /usr/share/maven/ref into ${MAVEN_CONFIG}
# so the inital ~/.m2 is set with expected content.
# Don't override, as this is just a reference setup
copy_reference_file()  {
    local root="${1}"
    local f="${2%/}"
    local logfile="${3}"
    local rel="${f/${root}/}" # path relative to /usr/share/maven/ref/
    echo "$f" >> "$logfile"
    echo "$f -> $rel" >> "$logfile"
    if [[ ! -e ${MAVEN_CONFIG}/${rel} || $f = *.override ]]
    then
        echo "copy $rel to ${MAVEN_CONFIG}" >> "$logfile"
        mkdir -p "${MAVEN_CONFIG}/$(dirname "${rel}")"
        cp -r "${f}" "${MAVEN_CONFIG}/${rel}";
    fi;
}
copy_reference_files() {
    local log="$MAVEN_CONFIG/copy_reference_file.log"

    if(sh -c "mkdir -p \"$MAVEN_CONFIG\" && touch \"${log}\"" > /dev/null 2>&1)
    then
        echo "--- Copying files at $(date)" >> "$log"
        find /usr/share/maven/ref/ -type f -exec bash -eu -c 'copy_reference_file /usr/share/maven/ref/ "$1" "$2"' _ {} "$log" \;
    else
        echo "Can not write to ${log}. Wrong volume permissions? Carrying on ..."
    fi
}

export -f copy_reference_file
copy_reference_files
unset MAVEN_CONFIG

DOCKER_SOCKET=/var/run/docker.sock 
DOCKER_GROUP=docker
USER=devops

if [ -s ${DOCKER_SOCKET} ]; then
    DOCKER_GID=$(stat -c '%g' ${DOCKER_SOCKET})

    if [[ DOCKER_GID -ne 0 ]]
    then
        sudo groupmod --gid ${DOCKER_GID} ${DOCKER_GROUP}
    fi
    sudo usermod --append --groups ${DOCKER_GID} ${USER}
    sudo chmod 777 /var/run/docker.sock
fi
export NVM_DIR="/tech/nvm" && \
    [ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh" && \
    [ -s "$NVM_DIR/bash_completion" ] && . "$NVM_DIR/bash_completion"


exec "$@"