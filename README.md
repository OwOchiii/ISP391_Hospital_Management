# ISP391 Hospital Management System - Local Setup Guide

This guide will help you clone the repository and run the hospital management system on your local machine.

## Prerequisites

- Java JDK 17 or higher
- Maven or Gradle
- Git
- IDE (IntelliJ IDEA recommended)

## Clone the Repository

```bash
git clone https://github.com/OwOchiii/ISP391_Hospital_Management.git
cd ISP391_Hospital_Management
```


## Open the Project in IntelliJ IDEA

1. Open IntelliJ IDEA
2. Select "Open" or "Import Project"
3. Navigate to the cloned repository folder
4. Select the project's root folder and click "Open"
5. If prompted, select to open as a Maven/Gradle project

## Configure the Database

1. Update the database configuration in `application.properties` or `application.yml` file located in `src/main/resources/`
2. Make sure your database server is running

## Build and Run the Project

### Using IntelliJ IDEA

1. Wait for IntelliJ to index and download dependencies
2. Find the main application class (likely named `Application.java` or similar)
3. Right-click on it and select "Run"

### Using Maven from Terminal

```bash
mvn clean install
mvn spring-boot:run
```

### Using Gradle from Terminal

```bash
./gradlew clean build
./gradlew bootRun
```

## Access the Application

Once the application is running, open your browser and navigate to:

```
http://localhost:8090
```

## Login Credentials

Use the credentials provided by your team lead or check the database for existing user accounts.

## Troubleshooting

- If you encounter dependency issues, try refreshing the Maven/Gradle project
- For database connection issues, verify your database is running and credentials are correct
- If the application fails to start, check the console for error messages

## Project Structure

- Controllers: `src/main/java/orochi/controller/`
- Services: `src/main/java/orochi/service/`
- Models: `src/main/java/orochi/model/`
- Templates: `src/main/resources/templates/`

For more information, contact the project team lead though discord (_owochi_).

Note: The Maven wrapper has not been thoroughly tested and may cause errors. It is recommended to use Gradle for building and running the project.
