#!/bin/bash
# Parse command line arguments
WORKING_DIR=$1
PEER_ID=$2

# Move to the working directory
cd $WORKING_DIR

# Build PeerProcess
javac -cp lib/log4j-api-2.19.0.jar:lib/log4j-core-2.19.0.jar:. *.java

# Run PeerProcess
LOG_FILE_PATH="${WORKING_DIR}/peer_${PEER_ID}/log_peer_${PEER_ID}.log"
nohup java -cp lib/log4j-api-2.19.0.jar:lib/log4j-core-2.19.0.jar:. -DlogFilePath=$LOG_FILE_PATH PeerProcess $PEER_ID > nohup.out 2>&1 &