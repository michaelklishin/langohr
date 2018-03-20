# Langohr, a feature-rich Clojure RabbitMQ client

Langohr is a [Clojure RabbitMQ client](http://clojurerabbitmq.info) that embraces [AMQP 0.9.1 Model](http://www.rabbitmq.com/tutorials/amqp-concepts.html).


## Project Goals

 * Embrace [AMQP 0.9.1 Model](http://www.rabbitmq.com/tutorials/amqp-concepts.html). Follow Java client's API conventions instead of inventing new overly opinionated ones
 * Be well documented. Use [Ruby amqp gem guides](http://rubyamqp.info) as a foundation.
 * Be well tested.
 * Error handling and recovery should be well covered
 * Support all of the RabbitMQ features, include [extensions to AMQP 0.9.1](http://www.rabbitmq.com/extensions.html).
 * Make error handling and recovery easier

We've learned a lot from over 6 years history of the [Ruby amqp
gem](http://rubyamqp.info), [Bunny](http://rubybunny.info), and RabbitMQ Java
client development and try to apply this experience to Langohr design.

## Project Anti-Goals

Here is what Langohr *does not* try to be:

 * A replacement for the RabbitMQ Java client
 * Sugar-coated API for task queues that hides all the protocol machinery from the developer
 * A port of Bunny to Clojure


## Artifacts

Langohr artifacts are [released to Clojars](https://clojars.org/com.novemberain/langohr). If you are using Maven, add the following repository
definition to your `pom.xml`:

``` xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

### The Most Recent Release

With [Leiningen](http://leiningen.org):

[![Clojars Project](http://clojars.org/com.novemberain/langohr/latest-version.svg)](http://clojars.org/com.novemberain/langohr)


With Maven:

``` xml
<dependency>
  <groupId>com.novemberain</groupId>
  <artifactId>langohr</artifactId>
  <version>5.0.0</version>
</dependency>
```


## Documentation & Examples

If you are only starting out, please see our [Getting Started guide](http://clojurerabbitmq.info/articles/getting_started.html).

[Documentation guides](http://clojurerabbitmq.info):

 * [AMQP 0.9.1 Concepts](http://www.rabbitmq.com/tutorials/amqp-concepts.html)
 * [Connecting To The Broker](http://clojurerabbitmq.info/articles/connecting.html)
 * [Queues and Consumers](http://clojurerabbitmq.info/articles/queues.html)
 * [Exchanges and Publishing](http://clojurerabbitmq.info/articles/exchanges.html)
 * [Bindings](http://clojurerabbitmq.info/articles/bindings.html)
 * [Durability](http://clojurerabbitmq.info/articles/durability.html)
 * [TLS/SSL](http://clojurerabbitmq.info/articles/tls.html)


### API Reference

For existing users, there is [API reference](http://reference.clojurerabbitmq.info).


### Code Examples

Several code examples used in the guides are kept in [a separate Git repository](https://github.com/clojurewerkz/langohr.examples).

Our [test suite](https://github.com/michaelklishin/langohr/tree/master/test/langohr/test) also can be used for code examples.


## Supported Clojure Versions

Langohr requires Clojure 1.6+. The most recent
stable release is highly recommended.


## Supported RabbitMQ Versions

Langohr depends on RabbitMQ Java client 3.x and requires
RabbitMQ versions 3.3 and later.


## Project Maturity

Langohr has been around since 2011. The API is stable.



## Community

[Langohr has a mailing
list](https://groups.google.com/forum/#!forum/clojure-rabbitmq). Feel
free to join it and ask any questions you may have.

To subscribe for announcements of releases, important changes and so
on, please follow [@ClojureWerkz](https://twitter.com/#!/clojurewerkz)
on Twitter.


## Langohr Is a ClojureWerkz Project

Langohr is part of the group of libraries known as [ClojureWerkz](http://clojurewerkz.org), together with

 * [Elastisch](https://clojureelasticsearch.info)
 * [Cassaforte](https://clojurecassandra.info)
 * [Monger](https://clojuremongodb.info)
 * [Welle](https://clojureriak.info)
 * [Neocons](https://clojureneo4j.info)
 * [Quartzite](https://clojurequartz.info)
 * [Titanium](https://titanium.clojurewerkz.org)



## Continuous Integration

[![Continuous Integration status](https://secure.travis-ci.org/michaelklishin/langohr.png)](http://travis-ci.org/michaelklishin/langohr)
[![Dependencies Status](http://jarkeeper.com/michaelklishin/langohr/status.png)](http://jarkeeper.com/michaelklishin/langohr)


## Development

See [CONTRIBUTING.md](https://github.com/michaelklishin/langohr/blob/master/CONTRIBUTING.md).


## License

Copyright (C) 2011-2018 Michael S. Klishin and the ClojureWerkz Team.

Double licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) (the same as Clojure) or
the [Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
