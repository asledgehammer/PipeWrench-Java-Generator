#!/bin/bash
set -e
steamcmd +force_install_dir $PWD/pzserver/ +login anonymous +app_update 380870 -beta ${1:-stable} validate +quit
