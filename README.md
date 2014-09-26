# fhirplace

FHIR server implementation

## Installation

Install docker

Let FRONTEND be absolute path to your frontend project that will use fhirplace

```
sudo docker.io run -d --name=fhirbase -t -p 5432 fhirbase/fhirbase:latest
sudo docker.io run -d --name=fhirplace -p 3000:3000 -v FRONTEND:/app --link fhirbase:db -t -i fhirbase/fhirplace:latest

curl http://localhost:3000/Patient/_search

```

## Usage

Root for you FRONTEND project: http://localhost:3000/app/
Simple access to fhirplace server: http://localhost:3000/fhirface/

## License

Copyright © 2014 FIXME

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
