# Event Sourcing Workshop

## Setup
Slices are defined as packages in the root package (as specified in the generator).

Find the exercises here:
https://nebulit-exercises.vercel.app/

Find the Swagger UI here:
http://localhost:8080/swagger-ui/index.html

Find the UI here:
https://eventsourcing-workshop-ui.vercel.app/

Run Code Generation:
```
docker run -ti -p 3001:3000 -v $PWD:/workspace -e HOST_WORKSPACE=$PWD --name codegen --rm nebulit/codegen
```

## Todos after the initial generation
The code contains TODOs that mark the places which need to be adapted. The generator makes certain basic assumptions (for example, aggregate IDs are UUIDs).

If these assumptions are changed, the code may not compile immediately and will need minor adjustments.

Your code guidelines take precedence, so it is expected that the code does not compile right away (however, only small adjustments should be necessary).

## Starting the application
To start the service, the `ApplicationStarter` class in `src/test/java` can be used. Why is it located in test?

This class starts the complete environment (including Postgres and, if necessary, Kafka via TestContainers).

## Package structure

Events are located in the events package.

Aggregates are located in the domain package.

Each slice has its own isolated package.

The common package contains several interfaces for the general structure.

## Tech stack

- Java 25
- Spring Boot 3.5
- Axon Framework 4.12
- Spring Modulith 1.4
- PostgreSQL
- Flyway (migrations)
- Testcontainers
