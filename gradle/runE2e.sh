#!/bin/bash

# Define the commands to run in each pane
COMMAND1="./gradlew runDockerE2eDev --no-daemon"
COMMAND2="./gradlew runWebAppNpmStartE2eDev --no-daemon"  # Replace with your second command
COMMAND3="./gradlew openBillingE2eDev --no-daemon"        # Replace with your first command
COMMAND4="./gradlew server-app:bootRun --args='--spring.profiles.active=e2e,e2e-billing' -Dorg.gradle.jvmargs='-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005'"

SESSION_NAME="tolgee_e2e"

# Check if the session exists
tmux has-session -t $SESSION_NAME 2>/dev/null

if [ $? != 0 ]; then
    # Create a new session and run COMMAND1 in the first pane
    tmux new-session -d -s $SESSION_NAME -n pane1
    tmux send-keys -t $SESSION_NAME:0 "$COMMAND1" C-m

    # Split horizontally and run COMMAND2
    tmux split-window -h -t $SESSION_NAME:0
    tmux send-keys -t $SESSION_NAME:0.1 "$COMMAND2" C-m

    # Split vertically and run COMMAND3
    tmux split-window -v -t $SESSION_NAME:0.0
    tmux send-keys -t $SESSION_NAME:0.2 "$COMMAND3" C-m

    # Split vertically and run COMMAND4
    tmux split-window -v -t $SESSION_NAME:0.1
    tmux send-keys -t $SESSION_NAME:0.3 "$COMMAND4" C-m
fi

# Attach to the session
tmux attach-session -t $SESSION_NAME
