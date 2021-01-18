# Open Data Blockchain (ODBl)
- Blockchain-based network for the trustworthy publication and integration of Open Data
- Based on DCAT and Practical Byzantine Fault Tolerance (PBFT)


# Prerequisites
- Java JDK 11
- Maven
- Docker and Docker Compose

# Build and Run 
```bash
$ cp config.sample.json config.json
```
- Adjust the config.json to your needs
```bash
$ mvn clean package -DskipTests
$ java -jar target/odbl-fat.jar
```
- Browse to `http://localhost:8080`

# Build and Run with Docker 
- Build the Docker image

```bash
$ mvn clean package -DskipTests
$ docker build -t=odbl-node .
```
- Start the application
```bash
$ docker-compose -f docker-compose.node.yml up -d
```

# Start and Test a Local Mini Network
- Requirements: Built Docker image
- Starts a small network with 4 publisher nodes and one authority node:

```bash
$ docker-compose -f docker-compose.network.yml up -d
```
- Register a node with the Authority
- Get the identity card:
```bash
$ curl --location --request POST "localhost:8081/admin/node" --header "Content-Type: application/json" --data-raw "{\"action\": \"identityCard\"}"
```
- Add the peer:
```bash
$ curl --location --request POST "localhost:5000/admin/addpeer" --header "Content-Type: text/plain" --data-raw "eyJub2RlSUQiOiJub2RlMDEiLCJuYW1lIjoiTm9kZSAwMSIsInVybCI6Imh0dHA6Ly9vZGJsLW5vZGUwMTo4MDgwIiwic2lnbmF0dXJlIjoiZGNyN1pjbHNOV1V1Q1RjRlp5YnV3VWNmZHZNQlJKQTVJczFmM2I2OUN4WXYyK2tsdEcxL01VVTQ5UkRMc0UrT3ZnTldHbVZWaFNVUHMxbFI4Sjc5RStyVDI3eGZRamFjQ3VvQ0FLaHcwVjU4Y0h4aXhjVmxkaUd2WTJwZGUxWTdhTU8ycExpSmtrbG9aa0JhdXZQcEdwRnluenpaQzc4T3JYQ2FEM3VuaGUrTGRLSFFHbkxTU09tWEJjSVdYY2x0elZXSTB6ME5tcDI1YnYvMUpQL2NKL0pOZFI5K25FUEdJb2NsR0NiZ3NoMGNwcUQ3THdjRWs5WDFRRVdPYVRON2U4Y1JQTFJFaE93Tnhib1E1SmRISDRMVkNseVNhWTVkaDFPUGQvclpnMXE4QjMxL0lWOGY2WXpOQVA2bk5nN2huWHpNaG5TcGYyblpUWjdSbUF4Q2NRPT0ifQ=="
```
- Important: You need to repeat the last two steps for each node!
- Check if the key exchange was successful, e.g. with:
```bash
$ curl http://localhost:8081/status
$ curl http://localhost:5000/node/peers
```
- Issue a new transaction:
```bash
$ curl --location --request POST "localhost:8081/node/transactions" --header "Content-Type: application/json"  --data-binary "@examples/example-dataset.json"
```
- Check if a new block/dataset was created:
```bash
$ curl http://localhost:8081/node/datasets
```
```bash
$ curl http://localhost:8081/node/blockchain
```

# Configuration
- Configuration can be passed via config.json or environment variable:

| Name | Description | Type | Example |
| ------ | ------ | ------ | ------ |
| PORT | The port of the node | number | 8080 |
| URL | The absolute URL of the node | string | http://localhost:8080 |
| NAME | The name of the node | string | Node XY |
| NODE_ID | The unique id of the node | string | node01 |
| MODE | The mode of the node - publisher or authority | string | publisher
| MONGO_HOST | The host of the MongoDB | string | localhost | 
| MONGO_PORT | The port of the MongoDB | number | 27017 |
| MONGO_DB | The name of the MoongoDB database | string | node_db |
| NETWORK_DELAY | Configuration of the network delay | json | {"enabled": false,"min": 0,"max": 1000} |
| INITIAL_PRIMARY | The initial primary | string | node01 |
| AUTO_SYNC | Enable syny on start-up | boolean | false |
| TACT | Period to check for new transactions in ms  | number | 4000 |
| AUTHORITY_NODE | URL of the authority node | string | http://localhost:5000 |
