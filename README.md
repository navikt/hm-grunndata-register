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

## Running in localhost

This use docker-compose.yml file located in hm-grunndata-db, startup database and kafka 

```
cd hm-grunndata-db
docker-compose up -d

```

Running the hm-grunndata-register:
```
export DB_DRIVER=org.postgresql.Driver
export DB_JDBC_URL=jdbc:postgresql://localhost:5432/register
export RAPIDSANDRIVERS_ENABLED=true
./gradlew build run
```

## Openapi is available here:
http://localhost:8080/swagger-ui


Create an admin user in the local database:

```
INSERT INTO user_v1(id, name, email,roles,attributes,token) 
VALUES (gen_random_uuid(), 'admin', 'admin@test.test', '["ROLE_ADMIN"]','{}', crypt('test123', gen_salt('bf', 8)));

```

Login to get JWT Token:
```
curl -v -X POST -H "Content-type: application/json" -d '{"username":"admin@test.test", "password":"test123"}' http://localhost:8080/login
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
}' http://localhost:8080/admin/api/v1/supplier/registrations
```

Create a supplier user in the local database:
```
 curl -v -X POST -H "Content-type: application/json" --cookie "JWT=$JWT" -d '{
  "name" : "User tester",
  "email" : "user1@test.test",
  "password" : "token123",
  "roles" : [ "ROLE_SUPPLIER" ],
  "attributes" : {"supplierId" : "<supplierId from previous request here>"}
}' http://localhost:8080/admin/api/v1/users

```

Login with this user:
```
curl -v -X POST -H "Content-type: application/json" -d '{"username":"user1@test.test", "password":"token123"}' http://localhost:8080/login
```

export JWT token for this user, we are going to use this user from now on:
``
export JWT=<copy and paste from the previous command>
``


Get current user:

```
curl -v -X GET -H "Content-type: application/json" --cookie "JWT=$JWT" http://localhost:8080/api/v1/user
```

Start a draft:
```
curl -v -X POST -H "Content-type: application/json" --cookie "JWT=$JWT" http://localhost:8080/api/v1/product/registrations/draft/test1
```

Get registrations (user):
```
curl -v -X GET -H "Content-type: application/json" --cookie "JWT=$JWT" http://localhost:8080/api/v1/product/registrations
```

Create product variant registrations (user):
```
curl -v -X POST -H "Content-type: application/json" --cookie "JWT=$JWT" http://localhost:8080/api/v1/product/registrations/draft/variant/<product id>/reference/<unik reference>
```

Update the draft:
```
curl -v -X PUT -H "Content-type: application/json" --cookie "JWT=$JWT" -d '<json_here>' http://localhost:8080/api/v1/product/registrations/<uuid>
```

Upload a media file for a product:
```
curl -v -X POST --cookie "JWT=$JWT" -F 'file=@path/to/file.jpg' http://localhost:8080/api/v1/media/product/file/<uuid>
```

Image versions can then be retrieved here:

````
http://localhost:8082/imageproxy/400d/register/<oid>/<uuid.jpg>
````

Upload many files at the same time:
````
curl -v -X POST --cookie "JWT=$JWT" -F 'files[]=@66131.jpg' -F 'files[]=@66131_4.jpg' http://localhost:8080/api/v1/media/product/files/<uuid>
````