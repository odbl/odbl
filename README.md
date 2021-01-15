# Open Data Blockchain (ODBl)

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

# Build and Run with Docker Compose
- Build the Docker image
```bash
$ mvn clean package
$ docker build -t=piveau-odbl .
```
- Start the application
```bash
$ docker-compose.exe -f docker-compose.node.yml up -d
```

# Configuration
...