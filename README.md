# SGRouter Graph Builder Service
GAE service to build a graph describing Singapore's public transit system, export it to a SQLite database and upload it to Google Cloud Storage.

## Technical Overview
Language: Java11
Framework: Spring Boot Web
GCP Products:
- App Engine
- Datastore
- Cloud Storage
- Directions API

## Prerequisites
- Java11
- GCP account setup for App Engine, Datastore, Cloud Storage and Directions API

## Wiki
https://github.com/jloh02/SGRouter/wiki/Graph-Builder-Service-Overview

## Installing
Clone the entire repository including other services:
```bash
git clone https://github.com/jloh02/SGRouter
```

Alternatively, only clone this submodule:
```bash
git clone https://github.com/jloh02/SGRouter-Graph-Builder
```

## Setup
### resources/application.yml
Set the following line for a GCloud API key to be used locally. This API key should be IP restricted and have permissions to access the Directions API.
```yml
gmap:
  local-api-key: "<insert_api_key_here>"
```

### GCP Datastore
API keys for server side code should be stored in the GCloud Key-Value pair Datastore.

Use the following naming for your Datastore
```yml
namespace: sgrouter
kind: keys
```

Store the following keys:
| Name/ID          | value                   |
| ---------------- | ----------------------- |
| DATAMALL_API_KEY | "<datamall_access_key>" |
| GMAP_API_KEY     | "<gmap_api_key>"        |

DATAMALL_API_KEY: Obtained from <https://datamall.lta.gov.sg/content/datamall/en.html>
GMAP_API_KEY: A different API key with permissions to access Directions API without IP restriction

#### Authentication
For server-side testing, ensure your App Engine default IAM account has access to Cloud Datastore.

For client-side testing, please remember to authenticate using your owner IAM account json: Refer to <https://cloud.google.com/docs/authentication/production>