#!/bin/bash
# Wipes all Docker data for a total reset.
cd ..
# Finds and kills locks in the project folders (tokens/config)
sudo find . -name "SingletonLock" -delete
docker system prune -a --volumes -f
docker ps -a
docker images
docker volume ls