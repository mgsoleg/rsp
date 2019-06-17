@echo off
echo "Starting RSP game server.... "
java -cp libs/* com.rsp.Main -inPort 777 -outPort 666 -poolSize 1048576