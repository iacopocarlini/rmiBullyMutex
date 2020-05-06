# RMI Bully + Mutual Exclusion
Bully coordinator election algorithm and mutual exclusion to use a resource in a distributed system using Java RMI  
Resource usage is just simulated with a sleep, there is space for a personal implementation.

# Execution
Launch every Node with command line arguments: [node ID] [seed]  
If seed equals to 1 the node waits until the last one is launched  
To launch the last one, run it setting seed to 2
