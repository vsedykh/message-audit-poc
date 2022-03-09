Steps to run:
1. Clone repo with demo apps
```
git clone https://github.com/vsedykh/message-audit-poc.git
```
2. Clone git with docker compose files for infra:
```
git clone https://github.com/stn1slv/docker-envs.git
```
3. Open terminal, switch to docker-envs folder and run docker compose with infra containers
```
docker-compose -f compose.yml -f kafka/compose-cp.yml -f elasticsearch/compose-opendistro.yml -f jaeger/streaming.yml -f kibana/compose-opendistro.yml up --remove-orphans
```
4. Open another terminal, switch to message-audit-poc folder and build projects
```
./build.sh
```
5. Run demo apps (replase $PATH_TO_DOCKER_ENVS to full or relative path to docker-envs folder)
```
docker-compose -f $PATH_TO_DOCKER_ENVS/compose.yml -f compose.yml up
```
6. Make test request using following request:
```
curl --location --request POST 'localhost:8080/product' \
--header 'X-Product-Id: 123' \
--header 'Content-Type: application/json' \
--data-raw '{"test":"test","fail":false,"fail2":false,"filterOut":true}'
```
You can change parameters fail, fail2, filterOut to generate error or interrupt processing in the middle

7. Go to kibana and create index pattern using message-audit-2022-02-28 index name. Choose startTimestamp as timeField
8. Now you can make queries or visualize data based on message audit message