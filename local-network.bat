call mvn package -DskipTests
set PORT=8081
set MONGO_DB=node02
set NODE_ID=node02
set NAME=Node 02
set URL=http://localhost:8081
set PIVEAU_LOG_LEVEL=DEBUG
set TACT=4000
set NETWORK_DELAY={"enabled": true,"min": 0,"max": 3000}
Taskkill /IM java.exe /FI "WINDOWTITLE eq odbl-node02" /F
start "odbl-node02" java -jar target/odbl-fat.jar
set PORT=8082
set MONGO_DB=node03
set NODE_ID=node03
set NAME=Node 03
set URL=http://localhost:8082
set PIVEAU_LOG_LEVEL=DEBUG
set TACT=4000
set NETWORK_DELAY={"enabled": true,"min": 0,"max": 3000}
Taskkill /IM java.exe /FI "WINDOWTITLE eq odbl-node03" /F
start "odbl-node03" java -jar target/odbl-fat.jar
set PORT=8083
set MONGO_DB=node04
set NODE_ID=node04
set NAME=Node 04
set URL=http://localhost:8083
set PIVEAU_LOG_LEVEL=DEBUG
set TACT=4000
set NETWORK_DELAY={"enabled": true,"min": 0,"max": 3000}
Taskkill /IM java.exe /FI "WINDOWTITLE eq odbl-node04" /F
start "odbl-node04" java -jar target/odbl-fat.jar