#!/bin/bash

if [ "$TRAVIS_REPO_SLUG" == "TomasMikula/WellBehavedFX" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]; then
  if [[ $(gradle -q getVersion) != *SNAPSHOT* ]]; then
      echo 'Travis can only publish snapshots.'
      exit 0
  fi

  echo -e "Starting publish to Sonatype...\n"

  gradle uploadArchives -PnexusUsername="${NEXUS_USERNAME}" -PnexusPassword="${NEXUS_PASSWORD}"
  RETVAL=$?

  if [ $RETVAL -eq 0 ]; then
    echo 'Completed publish!'
  else
    echo 'Publish failed.'
    return 1
  fi

fi
