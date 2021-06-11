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

## Installing
First clone this subdirectory of the repository:
```
git clone --depth 1 --filter=blob:none --sparse https://github.com/jloh02/SGRouter
cd SGRouter
git sparse-checkout set graph_builder_service
```

Alternatively, clone the entire repository:
```
git clone https://github.com/jloh02/SGRouter
```

## Wiki
https://github.com/jloh02/SGRouter/wiki/Graph-Builder-Service-Overview