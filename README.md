# What is Langohr

Langohr is a Clojure wrapper around the RabbitMQ Java client that embraces [AMQP 0.9.1 Model](http://bitly.com/amqp-model-explained)
and does not try to hide it behind many layers of DSLs. It is experimental only in the sense that API is not completely
locked down at this point: otherwise, it is solid and is used very actively to develop commercial products that involve thousands of nodes
communicating over AMQP.


## Project Goals

 * Embrace [AMQP 0.9.1 Model](http://bitly.com/amqp-model-explained). Follow Java client's API conventions instead of inventing new overly opinionated ones
 * Provide additional functions/protocols where it actually saves time (we learned a lot from 3 years history of the [Ruby amqp gem](https://github.com/ruby-amqp/amqp) development)
 * Be well documented. Two example READMEs do not cut it
 * Strict TDD development style (with tests sometimes being freeform examples first)
 * Support all of the [RabbitMQ extensions to AMQP 0.9.1](http://www.rabbitmq.com/extensions.html)
 * Provide support for testing of AMQP applications, including asynchronous workflows
 * Provide additional batteries such as CLI interface to AMQP operations

## Project Anti-Goals

Here is what Langohr *does not* try to be:

 * A replacement for the RabbitMQ Java client
 * Sugar-coated API for task queues that hides all the AMQP machinery from the developer
 * A port of Ruby amqp gem to Clojure


## Supported Clojure versions

Langohr is built from the ground up for Clojure 1.3 and up.


## Supported RabbitMQ versions

Langohr depends on RabbitMQ Java client 2.6.x and is known to work with RabbitMQ versions 2.5.1 and later.


## This is a Work In Progress

Langohr is a work in progress and until 1.0 is released with documentation and tutorials, there is nothing to
see here, really.


## Artifacts

Snapshot artifacts are [released to Clojars](https://clojars.org/com.novemberain/langohr) every few days.

With Leiningen:

    [com.novemberain/langohr "1.0.0-SNAPSHOT"]


With Maven:

    <dependency>
      <groupId>com.novemberain</groupId>
      <artifactId>langohr</artifactId>
      <version>1.0.0-SNAPSHOT</version>
    </dependency>


## Continuous Integration

[![Continuous Integration status](https://secure.travis-ci.org/michaelklishin/langohr.png)](http://travis-ci.org/michaelklishin/langohr)


CI is hosted by [travis-ci.org](http://travis-ci.org)


## The Road to 1.0

Langohr is slowly approaching 1.0 release. A few remaining items before the release are

 * Documentation guides.
 * Test suite cleanup.
 * Some stress tests to set baseline performance expectations.

We expect 1.0 to be released in the 4th quater of 2011.


## License

Copyright (C) 2011 Michael S. Klishin

Distributed under the Eclipse Public License, the same as Clojure.
