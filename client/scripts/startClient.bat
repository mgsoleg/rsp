@echo off
echo "Starting RSP game server.... "
java -cp libs/* com.rsp.client.Main  -serverHost localhost -serverPort 777 -userPort 555 -userName %1

