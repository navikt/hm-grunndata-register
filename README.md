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
