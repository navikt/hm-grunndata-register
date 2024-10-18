# hm-grunndata-register
Grunndata registrering

## Autentisering

Alle brukerer må være først registrert i databasen med epost og passord. Dette brukes under innlogging,
Api'et leverer en JWT token i cookie header, dette brukes som et autentiseringstoken for hvert kall til API'et.

## Authorisering

To type brukerer støttes, leverandør og interne admin bruker med tilsvarende roller: "ROLE_SUPPLIER" og "ROLE_ADMIN".
Leverandørbruker har også en attribute SUPPLIER_ID som forteller hvilken leverandør de tilhører. 
Leverandører har kun tilgang til produktene som har samme SUPPLIER_ID, mens admin bruker med ROLE_ADMIN har tilgang til 
alle produkter.

## Registreringsflyt
Leverandør/admin logger inn, oppretter en draft, og fyller inn data. Dette blir så lagret ved å kalle en POST til api'et.
Ser "aGoodDayRegistrationScenarioTest" i klassen ProductRegistrationAdminApiTest for mer detaljer i hvordan 
denne flyten fungerer.

## Administrering
Det er kun admin som kan godkjenne et produkt. Produktet blir ikke publisert før etter godkjennning. 
Flagget AdminStatus settes til APPROVED når produktet blir godkjent. 

# Development

Login in to gcloud and run naisdevice in the background, authenticate to google docker repository:

```
gcloud auth login (if you have not done it yet)
gcloud auth configure-docker europe-north1-docker.pkg.dev
```

### Using test database for local development:

First Get the latest database dump from google cloud bucket storage, after that drop previous "register" database,
default localhost postgres user is "postgres" and password is "postgres":

```
cd hm-grunndata-register
docker-compose -f docker-compose-for-backend.yml up

```

Then create a new database and user:

```
psql -h localhost -U postgres
DROP DATABASE register;
CREATE DATABASE register owner register;

```
Then dump the database from the downloaded file:

```
gunzip register-db-dump.gz
psql -h localhost -U register -f register-db-dump
```

## Running in localhost, for frontend developing.


This will run register, database, kafka, media and proxy in the background. 

```
cd hm-grunndata-register
docker-compose up -d

```

Register should be running on: http://localhost:8080/admreg/swagger-ui


## Running hm-grunndata-register for backend developing:

If you want to do backend development, hm-grunndata-register has to run separately.
```
cd hm-grunndata-register
docker-compose -f docker-compose-for-backend.yml up -d

export DB_DRIVER=org.postgresql.Driver
export DB_JDBC_URL=jdbc:postgresql://localhost:5432/register
export KAFKA_BROKERS=localhost:29092
export RAPIDSANDRIVERS_ENABLED=true
export SERVER_PORT=8080

./gradlew build run
```
hm-grunndata-register is now running here: http://localhost:8080/admreg/swagger-ui

## Openapi is available here:
http://localhost:8080/admreg/swagger-ui

### Examples of using the api:
Login into local postgresql to create an admin user:
psql -h localhost -U register, using password "register", then run these commands:

```
INSERT INTO user_v1(id, name, email,roles,attributes,token) 
VALUES (gen_random_uuid(), 'admin', 'admin@test.test', '["ROLE_ADMIN"]','{}', crypt('test12345', gen_salt('bf', 8)));

```

Login to get JWT Token:
```
curl -v -X POST -H "Content-type: application/json" -d '{"username":"admin@test.test", "password":"test12345"}' http://localhost:8080/admreg/login
```

export JWT token:
``
export JWT=<copy and paste from the previous command>
``

Create a supplier in the local database:
```
curl -v -X POST -H "Content-type: application/json" --cookie "JWT=$JWT" -d '{
  "name": "Supplier AS",
  "draftStatus": "DONE",
  "supplierData": {
    "address": "address 1",
    "email": "supplier@test.test",
    "phone": "+47 12345678",
    "homepage": "https://www.hompage.no"
  }
}' http://localhost:8080/admreg/admin/api/v1/supplier/registrations
```

Create a supplier user in the local database:
```
 curl -v -X POST -H "Content-type: application/json" --cookie "JWT=$JWT" -d '{
  "name" : "User tester",
  "email" : "user1@test.test",
  "password" : "token123",
  "roles" : [ "ROLE_SUPPLIER" ],
  "attributes" : {"supplierId" : "<supplierId from previous request here>"}
}' http://localhost:8080/admreg/admin/api/v1/users

```

Login with this user:
```
curl -v -X POST -H "Content-type: application/json" -d '{"username":"user1@test.test", "password":"token123"}' http://localhost:8080/admreg/login
```

export JWT token for this user, we are going to use this user from now on:
``
export JWT=<copy and paste from the previous command>
``


Get current LoggedIn User:

```
curl -v -X GET -H "Content-type: application/json" --cookie "JWT=$JWT" http://localhost:8080/admreg/loggedInUser
```

All vendor (Leverandør) user need to use /vendor in the path, for admin role change this to /admin

Start a draft:
```
curl -v -X POST -H "Content-type: application/json" --cookie "JWT=$JWT" http://localhost:8080/admreg/vendor/api/v1/product/registrations/draft/test1
```

Get registrations (user):
```
curl -v -X GET -H "Content-type: application/json" --cookie "JWT=$JWT" http://localhost:8080/admreg/vendor/api/v1/product/registrations
```

Create product variant registrations (user):
```
curl -v -X POST -H "Content-type: application/json" --cookie "JWT=$JWT" http://localhost:8080/admreg/vendor/api/v1/product/registrations/draft/variant/<product id>/reference/<unik reference>
```

Update the draft:
```
curl -v -X PUT -H "Content-type: application/json" --cookie "JWT=$JWT" -d '<json_here>' http://localhost:8080/admreg/vendor/api/v1/product/registrations/<uuid>
```

Upload a media file for a product:
```
curl -v -X POST --cookie "JWT=$JWT" -F 'file=@path/to/file.jpg' http://localhost:8080/admreg/vendor/api/v1/media/product/file/<uuid>
```

Image versions can then be retrieved here:

````
http://localhost:8082/imageproxy/400d/register/<oid>/<uuid.jpg>
````

Upload many files at the same time:
````
curl -v -X POST --cookie "JWT=$JWT" -F 'files=@@path/to/file1.jpg' -F 'files=@path/to/file2.jpg' http://localhost:8080/admreg/vendor/api/v1/media/product/files/<uuid>
````


