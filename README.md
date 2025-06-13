# RepliHash: A Fault-Tolerant Key-Value Store

This is the official repository for the **RepliHash** project, developed as part of the Distributed Systems course, Fall Semester 2025 at Amrita Vishwa Vidyapeetham, Bengaluru.

RepliHash is a distributed key-value store inspired by [Amazon DynamoDB](https://aws.amazon.com/dynamodb/), supporting **automatic data partitioning**, **quorum-based replication**, and **fault tolerance** using consistent hashing. The project has been accepted for presentation at **ICSCSP-2025**.

**Author:**  
[Ojas Tyagi](mailto:ojastyagi753@gmail.com)

## Features

- Consistent hashing-based dynamic partitioning
- Quorum-based replication model (N, R, W configurable)
- Failure detection and gossip-based membership management
- Actor-based concurrency using the Akka framework
- Fault-tolerant storage with persistent logs
- CLI-based node and client execution
- Fully tested using JUnit

## Dependencies

- Java 8+
- [Akka](https://akka.io/)
- [Gradle](https://gradle.org/)
- Internet connection (for first-time dependency download)

## Build Instructions

Use the following commands from the project root:

```bash
./gradlew node
./gradlew client
```
This will generate two executable JAR files in build/libs/:

node.jar: To start a node in the distributed system
client.jar: To interact with the system (read/write/leave)

⚠️ Make sure to configure parameters like N, R, W, and timeouts in 
[SystemConstants](src/main/java/it/unitn/ds1/SystemConstants.java) before compiling the code.

## Run
The project has 2 entry points:
* `Node`
* `Client`

The former is used to run a Node of the distributed database, the latter is used to query it.
You need to provide the following environment variables:

| Variable       | Description                           | Scope                       |
| -------------- | ------------------------------------- | --------------------------- |
| `HOST`         | IP address or hostname of the machine | Node/Client                 |
| `PORT`         | Port to bind the process              | Node/Client                 |
| `NODE_ID`      | Unique identifier for the node        | Node only                   |
| `STORAGE_PATH` | Directory for persisting data         | Node only (default: `/tmp`) |

To run the `Node`, run
```bash
java -jar build/libs/node.jar [COMMAND]
```

To run the `Client`, run
```bash
java -jar build/libs/client.jar [COMMAND]
```

### Example
The following example shows how to run the some nodes and make some queries.
Please note that some operating system or shell could use a slightly different syntax for environment variables.

```bash
# Bootstrap the system with the first node
HOST=127.0.0.1 PORT=20010 NODE_ID=10 java -jar build/libs/node.jar bootstrap

# Join a new node to the cluster
HOST=127.0.0.1 PORT=20020 NODE_ID=20 java -jar build/libs/node.jar join 127.0.0.1 20010

# Add another node
HOST=127.0.0.1 PORT=20030 NODE_ID=30 java -jar build/libs/node.jar join 127.0.0.1 20010

# Query for a missing key
HOST=127.0.0.1 PORT=30000 NODE_ID=0 java -jar build/libs/client.jar 127.0.0.1 20010 read 34

# Write a key-value pair
HOST=127.0.0.1 PORT=30000 NODE_ID=0 java -jar build/libs/client.jar 127.0.0.1 20020 write 34 hello

# Read the written key from another node
HOST=127.0.0.1 PORT=30000 NODE_ID=0 java -jar build/libs/client.jar 127.0.0.1 20030 read 34

# Make a node leave
HOST=127.0.0.1 PORT=30000 NODE_ID=0 java -jar build/libs/client.jar 127.0.0.1 20020 leave
```

### Assertions
The software contains assertions, in order to ensure a correct execution and catch bugs early.
By default, Java disables all assertions. You can decide to enable them with the `-ea`  option for the JVM:
```bash
java -ea -jar build/libs/node.jar [COMMAND]
```

## Test
We wrote some [JUnit](http://junit.org) test cases to automatically test both `Node` and `Client`.
You can run the test from the command line using Gradle:
```bash
./gradlew check
```
## Acknowledgement
This project draws inspiration from Amazon Dynamo and is developed under the course guidance of the Distributed Systems faculty.

📢 The project has been accepted for presentation at ICSCSP-2025 and showcases research in fault-tolerant, scalable distributed storage systems.

## License
This project is licensed under the MIT License.
A copy of the license is available in the [LICENSE](LICENSE) file.