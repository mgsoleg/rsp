@echo off
echo "Starting RSP game server.... "
java -XX:+UseG1GC -cp libs/* com.rsp.Main -inPort 777 -outPort 666 -poolSize 1048576