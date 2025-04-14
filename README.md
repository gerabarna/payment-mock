# payment-mock

## Assumptions
I have made the assumption that kafka and postgres are available as other running containers,
therefore I do not need to include configurations for these, or handle DB schema maintenance (with tools like flyway).

I run these as:
```
docker run -d -p 9092:9092 --name kafka apache/kafka:4.0.0
```
```
docker run -d \
    -e POSTGRES_PASSWORD='root' \ 
    -e POSTGRES_USER='root' \
    -p 5432:5432 --name postgres postgres:15
```
some of these are obviously not appropriate for production usage, but may be needed for local testing

To Create a proper schema for postgres ( flyway would be more elegant but needs some time to set up) please execute:
and then on the database
```
DROP TABLE IF EXISTS users, transactions;
DROP SEQUENCE IF EXISTS user_sequence, transaction_sequence;
CREATE SEQUENCE user_sequence START 1 INCREMENT 1;
CREATE TABLE users(
    id          INTEGER PRIMARY KEY DEFAULT nextval('user_sequence'),        -- over 4 billion users does not seem realistic
    balance     DECIMAL(32, 6)      DEFAULT 0,    -- actual precision may depend here on policy/main balance currency denominations
    currency    VARCHAR(3)          DEFAULT 'USD'   NOT NULL
);

CREATE SEQUENCE transaction_sequence START 1 INCREMENT 1;

CREATE TABLE transactions(
    id          BIGINT          PRIMARY KEY DEFAULT nextval('transaction_sequence'), -- let's be an optimistic and plan for a long future
    user_id     INTEGER         REFERENCES  users (id),
    request_id  varchar(50),
    inserted    timestamp       NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    amount      DECIMAL(32, 6)  NOT NULL,
    currency    VARCHAR(3)      NOT NULL
);
GRANT ALL PRIVILEGES ON TABLE users, transactions TO kibit;
GRANT ALL PRIVILEGES ON SEQUENCE user_sequence, transaction_sequence TO kibit;
```
You may need to grant access to your user to the new tables:
```
GRANT ALL PRIVILEGES ON TABLE users, transactions TO <user>;
GRANT ALL PRIVILEGES ON SEQUENCE user_sequence, transaction_sequence TO <user>;
```


I have also made the assumption that the accounts use USD as their currency. 
You did not ask to account for any currency related things I assume a single currency system is sufficient for 
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

# Usage
please edit the application properties to the appropriate values where kafka and postgres are located.
Once the application launches a new transaction can be submitted with:
POST localhost:8080/payment/transaction?userId=2&amount=4&currency=USD

# Further improvements

## Testing
Currently givenUserWithBalance_whenSmallerAmountTransactionComes_processedAsExpected runs fine on its own but fails
when all test in PaymentServiceHelperIntegrationTest are executed. 
The context is not properly re-set therefore between tests.

## Security
Currently there is no authorization on the transaction endpoint, obviously such an endpoint should be somehow authorized
in a real scenario