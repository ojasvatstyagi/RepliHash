# Distributed key-value store with data partitioning and replication 
This is the project of Distributed Systems 1, Fall Semester 2016-17, University of Trento.

The aim is to implement a distributes key-value store with automatic data partitioning and replication,
inspired by [Amazon DynamoDB](https://aws.amazon.com/dynamodb/).
The full description of the project can be found in the [project text](task/project_text.pdf) and
[project presentation](task/project_presentation.pdf).

Authors: [Andrea Zorzi](https://github.com/Andr35) & [Davide Pedranz](https://github.com/davidepedranz).

## Dependencies
The software is written in Java and uses the [Akka](http://akka.io/) framework.
We use [Gradle](https://gradle.org/) to build, test and run the project.
To build and run the project, you need only a working Java 8 JDK installed on the systems.
For the first build, the needed dependecies are downloaded from the Internet, so make 
sure to have a working Internet connection.

## Build
Run the following command from the project root:
```bash
./gradlew node
./gradlew client
```
This command will generate 2 JAR archives `build/libs/node.jar` and `build/libs/client.jar`
with all the dependencies needed to run the project.

Some parameters about the quorums, replication and timeouts are defined at compilation time,
as required by the assignment. Please make sure to adjust them in the
[SystemConstants](src/main/java/it/unitn/ds1/SystemConstants.java) before compiling the code.

## Run
The project has 2 entry points:
* `Node`
* `Client`

The former is used to run a Node of the distributed database, the latter is used to query it.
You need to provide the following environment variables:
 
Variable     | Scope                                                         | Notes
-------------|---------------------------------------------------------------|------------------
HOST         | The hostname of the machine where Node or Client is executed. |
PORT         | The port of the Node or the Client.                           |
NODE_ID      | A unique ID for the new node that will join the system.       | `Node` only.
STORAGE_PATH | The path where storage file for a Node will be saved.         | `Node` only. Optional, default `/tmp`.

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
# bootstrap the system
HOST=127.0.0.1 PORT=20010 NODE_ID=10 java -jar build/libs/node.jar bootstrap

# add a new node
HOST=127.0.0.1 PORT=20020 NODE_ID=20 java -jar build/libs/node.jar join 127.0.0.1 20010

# add another node
HOST=127.0.0.1 PORT=20030 NODE_ID=30 java -jar build/libs/node.jar join 127.0.0.1 20010

# make a query (for a missing key)
HOST=127.0.0.1 PORT=30000 NODE_ID=0 java -jar build/libs/client.jar 127.0.0.1 20010 read 34

# make a query (write a key)
HOST=127.0.0.1 PORT=30000 NODE_ID=0 java -jar build/libs/client.jar 127.0.0.1 20020 write 34 hello

# make a node leave
HOST=127.0.0.1 PORT=30000 NODE_ID=0 java -jar build/libs/client.jar 127.0.0.1 20020 leave

# make a query (now the key should be present)
HOST=127.0.0.1 PORT=30000 NODE_ID=0 java -jar build/libs/client.jar 127.0.0.1 20030 read 34
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
The command compiles the project and run all test cases. The result is shown in the standard output.
Please note that many test cases will spawn many times multiple Akka actors on your machine,
so the test suite can take some minutes to run.

## License
The source code is licences under the MIT license.
A copy of the license is available in the [LICENSE](LICENSE) file.
