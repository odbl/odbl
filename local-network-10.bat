call mvn package -DskipTests
set PORT=8081
set MONGO_DB=node02
set NODE_ID=node02
Taskkill /IM java.exe /FI "WINDOWTITLE eq odbl-node02" /F
start "odbl-node02" java -jar target/odbl-fat.jar
set PORT=8082
set MONGO_DB=node03
set NODE_ID=node03
Taskkill /IM java.exe /FI "WINDOWTITLE eq odbl-node03" /F
start "odbl-node03" java -jar target/odbl-fat.jar
set PORT=8083
set MONGO_DB=node04
set NODE_ID=node04
Taskkill /IM java.exe /FI "WINDOWTITLE eq odbl-node04" /F
start "odbl-node04" java -jar target/odbl-fat.jar
set PORT=8084
set MONGO_DB=node05
set NODE_ID=node05
Taskkill /IM java.exe /FI "WINDOWTITLE eq odbl-node05" /F
start "odbl-node05" java -jar target/odbl-fat.jar
set PORT=8085
set MONGO_DB=node06
set NODE_ID=node06
Taskkill /IM java.exe /FI "WINDOWTITLE eq odbl-node06" /F
start "odbl-node06" java -jar target/odbl-fat.jar
set PORT=8086
set MONGO_DB=node07
set NODE_ID=node07
Taskkill /IM java.exe /FI "WINDOWTITLE eq odbl-node07" /F
start "odbl-node07" java -jar target/odbl-fat.jar
set PORT=8087
set MONGO_DB=node08
set NODE_ID=node08
Taskkill /IM java.exe /FI "WINDOWTITLE eq odbl-node08" /F
start "odbl-node08" java -jar target/odbl-fat.jar
set PORT=8088
set MONGO_DB=node09
set NODE_ID=node09
Taskkill /IM java.exe /FI "WINDOWTITLE eq odbl-node09" /F
start "odbl-node09" java -jar target/odbl-fat.jar
set PORT=8089
set MONGO_DB=node10
set NODE_ID=node10
Taskkill /IM java.exe /FI "WINDOWTITLE eq odbl-node10" /F
start "odbl-node10" java -jar target/odbl-fat.jar
set PORT=8090
set MONGO_DB=node11
set NODE_ID=node11
Taskkill /IM java.exe /FI "WINDOWTITLE eq odbl-node11" /F
start "odbl-node11" java -jar target/odbl-fat.jar