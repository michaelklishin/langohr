## Changes between Langohr 1.0.0-beta6 and 1.0.0-beta7

No changes yet.


## Changes between Langohr 1.0.0-beta5 and 1.0.0-beta6

`1.0.0-beta6` has **BREAKING CHANGES**:

### langohr.basic/consume Delivery Handler Signature Change

`langohr.basic/consume`'s `:handle-delivery-fn` signature is now consistent with
that of `langohr.basic/subscribe`:

``` clojure
(fn [^Channel ch metadata ^bytes payload]
  )
```

This makes delivery handler signatures consistent across the board.



## Changes between Langohr 1.0.0-beta4 and 1.0.0-beta5

### More Connection Settings

`langohr.core/connect` now supports several more options:

 * `:ssl` (true or false): when true, Langohr will use the default SSL protocol (SSLv3) and the default (trusting) trust manager
 * `:ssl-context` (`javax.net.ssl.SSLContext`): SSL context to use to create connection factory
 * `:sasl-config` (`com.rabbitmq.client.SaslConfig`): use if you need to use a custom SASL config
 * `:socket-factory` (`javax.net.SocketFactory`): use if you need to use a custom socket factory


### Client Capabilities

Langohr now provides its capabilities to the broker so it's possible to tell the difference between
Langohr and the RabbitMQ Java client in the RabbitMQ Management UI connection information.

### Broker Capabilities Introspection

`langohr.core/capabilities-of` is a new function that returns broker capabilities as an immutable map,
e.g.

``` clojure
{:exchange_exchange_bindings true
 :consumer_cancel_notify true
 :basic.nack true
 :publisher_confirms true}
```

### Clojure 1.4 By Default

Langohr now depends on `org.clojure/clojure` version `1.4.0`. It is still compatible with Clojure 1.3 and if your `project.clj` depends
on 1.3, it will be used, but 1.4 is the default now.

We encourage all users to upgrade to 1.4, it is a drop-in replacement for the majority of projects out there.



## Changes between Langohr 1.0.0-beta3 and 1.0.0-beta4

### Payload is Now Longer Assumed to Be a String

`langohr.basic/publish` no longer assumes the payload is always a string. It can be anything the `langohr.conversion/BytePayload`
protocol is implemented for, by default byte arrays and strings.

### queue.declare :exclusive Default Value Change

`langohr.queue/declare` now uses default value for the `:exclusive` parameter as `false`. The reason for
this is that exclusive queues are deleted when connection that created them is closed. This caused
confusion w.r.t. non-auto-deleted queues being deleted in such cases.



## Changes between Langohr 1.0.0-beta2 and 1.0.0-beta3

### URI parsing

`langohr.core/settings-from` is a new public API function that parses AMQP and AMQPS connection URIs
and returns an immutable map of individual arguments. URI parsing is now delegated to the Java client
for consistency.


### RabbitMQ Java Client 2.8.6

RabbitMQ Java Client has been upgraded to version 2.8.6.


## Changes between Langohr 1.0.0-beta1 and 1.0.0-beta2

### Breaking change: message handler signature has changed

Previously message handlers registered via `langohr.consumers/subscribe` had the following
signature:

``` clojure
(fn [^QueueingConsumer$Delivery delivery ^AMQP$BasicProperties properties payload] ...)
```

starting with beta2, it has changed to be more Clojure friendly

``` clojure
(fn [^Channel ch metadata payload] ...)
```

All message metadata (both envelope and message properties) are now passed in as a single Clojure
map that you can use destructuring on:

``` clojure
(fn [^Channel ch {:keys [type content-type message-id correlation-id] :as metadata} payload] ...)
```

In addition, in explicit acknowledgement mode, ack-ing and nack-ing messages got easier because
consumer channel is now passed in.

It is important to remember that sharing channels between threads that publish messages is **dangerous**
and should be avoided. Ack-ing, nack-ing and consuming messages with shared channels is usually acceptable.



### RabbitMQ Java Client 2.8.x

RabbitMQ Java Client which Langohr is based on has been upgraded to version 2.8.1.

### Leiningen 2

Langohr now uses [Leiningen 2](https://github.com/technomancy/leiningen/wiki/Upgrading).
