docker stop italians && docker rm italians

docker run -d \
 --name italians \
 -p 8083:8080 \
 maesi/ch-open-18:0.0.1