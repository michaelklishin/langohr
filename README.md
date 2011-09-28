# What is Langohr

Langohr is an experimental Clojure wrapper around the RabbitMQ Java client that embraces [AMQP 0.9.1 Model](http://bitly.com/amqp-model-explained)
and does not try to hide it behind many layers of DSLs. It is experimental only in the sense that API is not completely
locked down at this point: it is used very actively to develop commercial products that involve thousands of nodes
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


## This is a Work In Progress

Langohr is very much a work in progress and without most of key AMQP operations being
supported, proper test suite and documentation guides, there is nothing to
see here, really.


## Continuous Integration

[![Continuous Integration status](https://secure.travis-ci.org/michaelklishin/langohr.png)](http://travis-ci.org/michaelklishin/langohr)


CI is hosted by [travis-ci.org](http://travis-ci.org)


## Usage

Since these are very early days of the library, it is fair to say that it is *completely unusable* to anyone
other than the author.



## License

Copyright (C) 2011 Michael S. Klishin

Distributed under the Eclipse Public License, the same as Clojure.
