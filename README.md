# movies-quarkus

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: <https://quarkus.io/>.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at <http://localhost:8080/q/dev/>.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `target/quarkus-app/lib/` directory.

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.jar.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar target/*-runner.jar`.

## Creating a native executable

You can create a native executable using:

```shell script
./mvnw package -Dnative
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using:

```shell script
./mvnw package -Dnative -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./target/movies-quarkus-1.0.0-runner`

If you want to learn more about building native executables, please consult <https://quarkus.io/guides/maven-tooling>.

## Testing file storage

`StorageResource` exposes `/api/v1/storage/files` to read and write plain-text files in the directory set by `storage.path`
(`/data` in the cluster, where the PVC is mounted; `target/storage` in dev mode).

```shell script
# Local (dev mode)
BASE=http://localhost:8080/api/v1/storage/files
# OpenShift
BASE=https://$(oc get route movies-quarkus -o jsonpath='{.spec.host}')/api/v1/storage/files

# Create a file (201 Created; 204 No Content if it already existed)
curl -i -X PUT -H 'Content-Type: text/plain' --data 'hello pvc' $BASE/test.txt

# List files
curl $BASE

# Read a file (404 if it does not exist)
curl $BASE/test.txt

# Delete a file (204 No Content)
curl -i -X DELETE $BASE/test.txt
```

To confirm the file is on the PVC, check it inside the pod:

```shell script
oc exec deploy/movies-quarkus -- cat /data/test.txt
```

## Provided Code

### REST

Easily start your REST Web Services!

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)