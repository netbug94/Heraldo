docker system prune -a --volumes -f

docker ps -a       # Should show zero containers
docker images      # Should show zero images
docker volume ls   # Should show zero volumes
