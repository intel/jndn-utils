# jndn-utils

This project is a collection of tools to simplify synchronous and asynchronous data transfer over the NDN network. It relies on the [NDN Protocol](https://named-data.net) and its associated [client library](https://github.com/named-data/jndn).

## Install
With Maven, add the following to your POM:
```
<dependency>
  <groupId>com.intel.jndn.utils</groupId>
  <artifactId>jndn-utils</artifactId>
  <version>RELEASE</version> <!-- or a specific version -->
</dependency>
```

## Build

To build, run `mvn install` in the cloned directory. Additionally, you may want
to run integration tests by running the `nfd-integration-tests` profile with 
a running NFD instance (see [pom.xml](https://github.com/01org/jndn-utils/blob/master/pom.xml) for more details);

## Use

This library provides `Client`, `Server` and `Repository` interfaces. These
are implemented in their respective `impl` packages and require Java 8. 

Use a `SimpleClient` or `AdvancedClient` (provides segmentation, retries, and streaming)
 to retrieve data from the network. For example:
```
// retrieve a single Data packet synchronously, will block until complete
Data singleData = SimpleClient.getDefault().getSync(face, name);

// retrieve segmented Data packets (i.e. with a last Component containing a segment number and a valid FinalBlockId) by name
CompletableFuture<Data> segmentedData = AdvancedClient.getDefault().getAsync(face, name);
```

Use `SimpleServer` or `SegmentedServer` to serve data on the network. For example:
```
// segment and serve Data packet under a specific prefix
RepositoryServer server = new SegmentedServer(face, prefix);
server.serve(largeDataPacket); // call face.processEvents() in an event loop

// add signatures; this pipeline stage will sign each Data packet prior to being encoded for transport
server.addPipelineStage(new SigningStage(keyChain, signingCertificateName));
```

For the full API, see the [Javadoc](http://01org.github.io/jndn-utils/).

## Logging

`jndn-utils` uses Java's default logging utilities (see http://docs.oracle.com/javase/7/docs/api/java/util/logging/package-summary.html). Most messages are logged with the FINER or FINEST status; one way to change this is to add a `logging.properties` file in the classpath with the following lines:
```
handlers=java.util.logging.ConsoleHandler
.level=FINEST
java.util.logging.ConsoleHandler.level=FINEST
```

## License
Copyright &copy; 2015, Intel Corporation.

This program is free software; you can redistribute it and/or modify it under the terms and conditions of the GNU Lesser General Public License, version 3, as published by the Free Software Foundation.

This program is distributed in the hope it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the [GNU Lesser General Public License](https://github.com/01org/jndn-utils/blob/master/LICENSE) for more details.
