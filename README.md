# Langohr, a feature-rich Clojure RabbitMQ client that embraces [AMQP 0.9.1 Model](http://bitly.com/amqp-model-explained)

Langohr is a [Clojure RabbitMQ client](http://clojurerabbitmq.info) that embraces [AMQP 0.9.1 Model](http://bitly.com/amqp-model-explained)
and does not try to hide it behind many layers of DSLs. It is pre-1.0 only in the sense that API is not completely
locked down at this point: otherwise, it is solid and is used very actively to develop commercial products that involve thousands of nodes
communicating with it.


## Project Goals

 * Embrace [AMQP 0.9.1 Model](http://bitly.com/amqp-model-explained). Follow Java client's API conventions instead of inventing new overly opinionated ones
 * Provide additional functions/protocols where it actually saves time (we learned a lot from 3+ years history of the [Ruby amqp gem](https://github.com/ruby-amqp/amqp) development)
 * Be well documented. Use [Ruby amqp gem guides](http://rubyamqp.info) as a foundation.
 * Strict TDD development style (with tests sometimes being freeform examples first)
 * Support all of the [RabbitMQ extensions to AMQP 0.9.1](http://www.rabbitmq.com/extensions.html)
 * Provide support for testing of AMQP applications, including asynchronous workflows
 * Provide additional batteries such as CLI interface to AMQP operations

## Project Anti-Goals

Here is what Langohr *does not* try to be:

 * A replacement for the RabbitMQ Java client
 * Sugar-coated API for task queues that hides all the AMQP machinery from the developer
 * A port of Ruby amqp gem to Clojure



## Documentation & Examples

If you are only starting out, please see our [Getting Started guide](http://clojurerabbitmq.info/articles/getting_started.html).

[Documentation guides](http://clojurerabbitmq.info) are incomplete but most of the content is there:

 * [AMQP Concepts](http://www.rabbitmq.com/tutorials/amqp-concepts.html)
 * [Conneciting To The Broker](http://clojurerabbitmq.info/articles/connecting.html)
 * [Queues and Consumers](http://clojurerabbitmq.info/articles/queues.html)
 * [Exchanges and Publishing](http://clojurerabbitmq.info/articles/exchanges.html)
 * [Bindings](http://clojurerabbitmq.info/articles/bindings.html)

The rest of the guides will be written eventually.

### API Reference

For existing users, there is [API reference](http://reference.clojurerabbitmq.info).


### Code Examples

Several code examples used in the guides are kept in [a separate Git repository](https://github.com/clojurewerkz/langohr.examples).

Our [test suite](https://github.com/michaelklishin/langohr/tree/master/test/langohr/test) also can be used for code examples.



## Community

[Langohr has a mailing list](https://groups.google.com/forum/#!forum/clojure-rabbitmq). Feel free to join it and ask any questions you may have.

To subscribe for announcements of releases, important changes and so on, please follow [@ClojureWerkz](https://twitter.com/#!/clojurewerkz) on Twitter.


## This is a Work In Progress

While the API is largely stabilized at this point, Langohr is a work in progress. Breaking API changes are not out of the question because
it is much less painful to do them when the library is still young. Also, not every
idea we have is implemented. Keep that in mind.


## Artifacts

With Leiningen:

    [com.novemberain/langohr "1.0.0-beta13"]


With Maven:

    <dependency>
      <groupId>com.novemberain</groupId>
      <artifactId>langohr</artifactId>
      <version>1.0.0-beta13</version>
    </dependency>


## Supported Clojure versions

Langohr is built from the ground up for Clojure 1.3 and up. The most recent stable release is highly
recommended.


## Supported RabbitMQ versions

Langohr depends on RabbitMQ Java client 3.0.x and thus should work
with RabbitMQ versions 2.0 and later.


## The Road to 1.0

Langohr is slowly approaching 1.0 release. A few remaining items before the release are

 * Finish [documentation guides](http://clojurerabbitmq.info)
 * Design error handling and recovery
 * Some stress tests to set baseline performance expectations
 * Finish HTTP API client support

We expect 1.0 to be released in 2013 (but not before [documentation guides](http://clojurerabbitmq.info) are finished).


## Langohr Is a ClojureWerkz Project

Langohr is part of the group of libraries known as [ClojureWerkz](http://clojurewerkz.org), together with
[Monger](https://github.com/michaelklishin/monger), [Neocons](https://github.com/michaelklishin/neocons), [Elastisch](https://github.com/clojurewerkz/elastisch), [Quartzite](https://github.com/michaelklishin/quartzite) and several others.



## Continuous Integration

[![Continuous Integration status](https://secure.travis-ci.org/michaelklishin/langohr.png)](http://travis-ci.org/michaelklishin/langohr)


CI is hosted by [travis-ci.org](http://travis-ci.org)


## Development

Langohr uses [Leiningen 2](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md). Make
sure you have it installed and then run tests against all supported Clojure versions using

    lein2 all test

Then create a branch and make your changes on it. Once you are done with your changes and all
tests pass, submit a pull request on Github.


## License

Copyright (C) 2011-2013 Michael S. Klishin

Distributed under the Eclipse Public License, the same as Clojure.
