# Langohr, a feature-rich Clojure RabbitMQ client that embraces [AMQP 0.9.1 Model](http://bitly.com/amqp-model-explained)

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



## Documentation & Examples

We are working on documentation guides & examples site for the 1.0 release. In the meantime, please refer to the [test suite](https://github.com/michaelklishin/langohr/tree/master/test/langohr/test) for code examples.



## Community

[Langohr has a mailing list](https://groups.google.com/forum/#!forum/clojure-rabbitmq). Feel free to join it and ask any questions you may have.

To subscribe for announcements of releases, important changes and so on, please follow [@ClojureWerkz](https://twitter.com/#!/clojurewerkz) on Twitter.


## This is a Work In Progress

While the API is largely stabilized at this point, Langohr is a work in progress. Documentation and tutorials site is still not published, not every
idea we have is implemented. Keep that in mind.


## Artifacts

With Leiningen:

    [com.novemberain/langohr "1.0.0-beta1"]


With Maven:

    <dependency>
      <groupId>com.novemberain</groupId>
      <artifactId>langohr</artifactId>
      <version>1.0.0-beta1</version>
    </dependency>

If you feel comfortable using snapshots:

With Leiningen:

    [com.novemberain/langohr "1.0.0-SNAPSHOT"]


With Maven:

    <dependency>
      <groupId>com.novemberain</groupId>
      <artifactId>langohr</artifactId>
      <version>1.0.0-SNAPSHOT</version>
    </dependency>

Snapshot artifacts are [released to Clojars](https://clojars.org/com.novemberain/langohr) every day.


## Supported Clojure versions

Langohr is built from the ground up for Clojure 1.3 and up.


## Supported RabbitMQ versions

Langohr depends on RabbitMQ Java client 2.7.x and thus should work with RabbitMQ versions 2.0 and later.


## The Road to 1.0

Langohr is slowly approaching 1.0 release. A few remaining items before the release are

 * Documentation guides.
 * Test suite cleanup.
 * Some stress tests to set baseline performance expectations.

We expect 1.0 to be released in 2012 (but not before documentation site is ready).


## Continuous Integration

[![Continuous Integration status](https://secure.travis-ci.org/michaelklishin/langohr.png)](http://travis-ci.org/michaelklishin/langohr)


CI is hosted by [travis-ci.org](http://travis-ci.org)


## Development

Langohr uses [Leiningen 2](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md). Make
sure you have it installed and then run tests against Clojure 1.3.0 and 1.4.0[-beta4] using

    lein2 with-profile test:1.4 test

Then create a branch and make your changes on it. Once you are done with your changes and all
tests pass, submit a pull request on Github.


## License

Copyright (C) 2011-2012 Michael S. Klishin

Distributed under the Eclipse Public License, the same as Clojure.
