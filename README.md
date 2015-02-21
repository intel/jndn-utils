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

## Use
Use `Client` or `SegmentedClient` to retrieve data from the network. For example:
```
Data data1 = Client.getDefault().getSync(face, name);
Data data2 = SegmentedClient.getDefault().getSync(face, name);
```

## License
© Copyright Intel Corporation. Licensed under LGPLv3, see [LICENSE](https://github.com/01org/jndn-mock/blob/master/LICENSE).
