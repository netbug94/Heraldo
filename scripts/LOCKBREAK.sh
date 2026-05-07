#!/bin/bash
# Restarts services and clears stuck Google locks.
cd ..
docker compose down
# Finds and kills locks in the project folders (tokens/config)
sudo find . -name "SingletonLock" -delete
docker compose up -d