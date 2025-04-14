# payment-mock

## Assumptions
I have made the assumption that kafka, vault and postgres are available as other running containers,
therefore I do not need to include configurations for these, or handle DB schema maintenance (with tools like flyway).

I run these as:
```
docker run -d -p 9093:9093 --name kafka apache/kafka:4.0.0
```
```
docker run -d \
    -e POSTGRES_PASSWORD='root' \ 
    -e POSTGRES_USER='root' \
    -p 5432:5432 --name postgres postgres:15
```
```
docker run -d --cap-add=IPC_LOCK \
    -e 'VAULT_DEV_ROOT_TOKEN_ID=myroot' \
    -e 'VAULT_DEV_LISTEN_ADDRESS=0.0.0.0:1234' \
    -p 1234:1234 \
    --name vault hashicorp/vault:1.19
```
some of these are obviously not appropriate for production usage, but may be needed for local testing

I have also made the assumption that the accounts use USD as their currency. 
You did not ask to account for any currency related things I assume a single currency system is sufficient for 
this small mock, but since we are talking about 'payments' completely omitting currencies felt wrong. 

## Tools
I have used the google style plugin for formatting.

I have used vault to store secrets. This was not specified, but some secret store would be necessary 
to securely store sensitive information, as sensitive information cannot really be removed from a 
repository without mangling histories, so it is good to have that from the start
