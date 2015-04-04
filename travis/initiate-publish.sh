#!/bin/bash

if [ "$TRAVIS_REPO_SLUG" == "TomasMikula/WellBehavedFX" ] && [ "$TRAVIS_PULL_REQUEST" == "false" ] && [ "$TRAVIS_BRANCH" == "master" ]; then
  openssl aes-256-cbc -k "$ENC_PWD" -in gradle.properties.enc -out gradle.properties
  openssl aes-256-cbc -k "$ENC_PWD" -in secring.gpg.enc -out secring.gpg

  echo -e "Starting publish to Sonatype...\n"

  gradle uploadArchives
  RETVAL=$?

  if [ $RETVAL -eq 0 ]; then
    echo 'Completed publish!'
  else
    echo 'Publish failed.'
    return 1
  fi

fi
