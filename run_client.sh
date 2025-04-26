#!/bin/bash

JAR_PATH="build/libs/client.jar"

# Write a key
HOST=127.0.0.1 PORT=30000 NODE_ID=0 java -jar $JAR_PATH 127.0.0.1 20010 write 101 "HelloDS"

# Read the key
HOST=127.0.0.1 PORT=30000 NODE_ID=0 java -jar $JAR_PATH 127.0.0.1 20020 read 101

# Write another key
HOST=127.0.0.1 PORT=30000 NODE_ID=0 java -jar $JAR_PATH 127.0.0.1 20030 write 202 "OjasRocks"

# Kill a node (simulate failure)
tmux send-keys -t dsnodes:0.2 C-c  # kills node at PORT=20030 (NODE_ID=3)

# Try reading the lost key (check quorum)
HOST=127.0.0.1 PORT=30000 NODE_ID=0 java -jar $JAR_PATH 127.0.0.1 20040 read 202
