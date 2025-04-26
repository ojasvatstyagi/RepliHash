#!/bin/bash
JAR_PATH="build/libs/node.jar"
LOG_DIR="logs"
SESSION="dsnodes"

# Clean up old logs and tmux session

tmux kill-session -t $SESSION 2>/dev/null

# Start tmux session with the bootstrap node
tmux new-session -d -s $SESSION "export PORT=20010 NODE_ID=1; java -jar $JAR_PATH bootstrap"
sleep 2

# Split vertically and start second node
tmux split-window -v "export PORT=20020 NODE_ID=2; java -jar $JAR_PATH join 127.0.0.1 20010"
sleep 2

# Split horizontally from pane 0 and start third node
tmux select-pane -t 0
tmux split-window -h "export PORT=20030 NODE_ID=3; java -jar $JAR_PATH join 127.0.0.1 20010"
sleep 2

# Split horizontally from pane 1 and start fourth node
tmux select-pane -t 1
tmux split-window -h "export PORT=20040 NODE_ID=4; java -jar $JAR_PATH join 127.0.0.1 20010"
sleep 2

# Arrange the panes nicely
tmux select-layout tiled

# Attach to the session
tmux attach-session -t $SESSION
