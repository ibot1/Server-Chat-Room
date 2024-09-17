## Desscription
This application provides the capability of one-to-one and many-to-many back and forth real-time communication between players.
For more details see PROBLEM.md.

## Requirements
- [Java 22](https://www.oracle.com/ng/java/technologies/downloads/)
- [Maven cli](https://maven.apache.org/download.cgi)
- [Install a Linux-emulator](https://www.cygwin.com/) for Windows only.

## How-To-Run
- `export java={JAVA_HOME_PATH}`
- `export mvn={MAVEN_PATH}`
- `cd {Project_PATH}`
- `mvn clean install`
- `chmod +x run.sh`
- `./run.sh`

## Improvements:
- For simplicity and a clean api look, used System.out.format instead of java.util.Logger/System.Logger.
- More Null validation checks and wrapping of compile time exceptions.
- Better command-line argument validation.
- Using websocket but requires a library like jetty or implement of its RFC.
- Making it more reactive, enable async and caching for performance.
- Register shutdown handlers.