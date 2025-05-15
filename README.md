# payment-mock

## Assumptions
I have made the assumption that kafka and postgres are available as other running containers.
For ease of use I have crated some simple configurations. please see compose.yml for these. Please make sure 
to run the appropriate version of the kafka container for your use case. ( Unfortunately depending on the network 
they need slightly different configurations) 
You can also run the containers manually as:

I run these as:
```
docker run -d -p 9092:9092 --name kafka apache/kafka:4.0.0
```
```
docker run -d \
    -e POSTGRES_PASSWORD='kibit' \ 
    -e POSTGRES_USER='kibit' \
    -p 5432:5432 --name postgres postgres:15
```
some of these are obviously not appropriate for production usage ( like using the db admin user for the db connection), 
but should do for local testing

if you choose to run the containers manually you will also need to create the schema, for this please check and execute
the [docker-db-schema-init.sql](docker-db-schema-init.sql) script file

I have also made the assumption that the accounts use USD as their currency. 
You did not ask to account for any currency related things. I assume a single currency system is sufficient for 
this small mock, but since we are talking about 'payments' completely omitting currencies felt wrong. 

## Tools
### code style
I have used the google style plugin for formatting.

### vault
I have planned to use vault to store secrets. This was not specified, but some secret store would be necessary 
to securely store sensitive information, as sensitive information cannot really be removed from a 
repository without mangling histories, so it is good to have that from the start. Unfortunately I did not have time to
add this

### dockerisation
I have containerised the application with the maven jib plugin. To create the docker image please execute:
```mvn compile com.google.cloud.tools:jib-maven-plugin:3.4.5:build```

this uploads the image into docker hub (registry.hub.docker.com) with the current settings, and assumes the credentials 
to be present in the maven settings.xml for the repository connection

I have also added a compose.yml for ease of use. As I have used jib to create the docker image, the actual application 
image needs to be created by the above command, but the pushed image can be used with the specifications in compose.yml

# Usage
please edit the application properties or the environment variables in compose.yml to the appropriate values where 
kafka and postgres are located in case you have a different setup.
Once the application launches a new transaction can be submitted with a POST request:
```
localhost:8080/payment/trasnfer?senderId=2&receiverId=4&amount=10&currency=USD
```

# Further improvements

## Clustering
Even though the solution was containerized, the current solution is only appropriate for a single instance deployment. 

To make the solution appropriate for multiple instances the locking solution would need to be transformed. 
The current setup could be directly updated to use distributed locks with minor adjustment
(for example Hazelcast has those). 

Another possibility would be to use the users table in the DB:
the user records by id could be locked to ensure correct processing, however this would be slower, and would
seriously interfere with user balance queries(assuming the system would have those).
Furthermore the point of this mock to demonstrate some Java technology usage, not postgres usage.

## Security
Currently there is no authorization on the transaction endpoint, obviously such an endpoint should be somehow authorized
in a real scenario