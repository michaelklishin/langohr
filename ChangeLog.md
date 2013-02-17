## Changes between Langohr 1.0.0-beta10 and 1.0.0-beta11

### HTTP API Client

Langohr `1.0.0-beta11` features initial bits of RabbitMQ HTTP API client
under `langohr.http`.


### Cheshire 5.x

[Cheshire](https://github.com/dakrone/cheshire) dependency has been upgraded to `5.0.1`


### More Convenient TLS Support

Langohr will now automatically enable TLS/SSL if provided `:port` is
`5671`.


### RabbitMQ Java Client 3.0.x

RabbitMQ Java Client has been upgraded to version `3.0.2`.


### langohr.exchange/declare-passive

`langohr.exchange/declare-passive` is a new function that performs passive
exchange declaration (checks if an exchange exists).

An example to demonstrate:

``` clojure
(require '[langohr.channel  :as lch])
(require '[langohr.exchange :as le])

(let [ch (lch/open conn)]
  (le/declare-passive ch "an.exchange"))
```

If the exchange does exist, the function works just like `langohr.exchange/declare`. If not,
an exception (`com.rabbitmq.client.ShutdownSignalException`, `java.io.IOException`) will be thrown.


## Changes between Langohr 1.0.0-beta9 and 1.0.0-beta10

### langohr.basic/reject now correctly uses basic.reject

langohr.basic/reject now correctly uses `basic.reject` AMQP method
and not `basic.ack`.

Contributed by @natedev.



## Changes between Langohr 1.0.0-beta8 and 1.0.0-beta9

`1.0.0-beta9` has **BREAKING CHANGES**:

### Return Handlers Body Now Passed as-is

Langohr no longer instantiates a string from the message body before passing it to
return listeners. The body will be passed as is, as an array of bytes.




## Changes between Langohr 1.0.0-beta7 and 1.0.0-beta8

`1.0.0-beta8` has **BREAKING CHANGES**:

### langohr.basic/get Return Value Change

`langohr.basic/get` now returns a pair of `[metadata payload]` to be consistent with what
delivery handler functions accept:

``` clojure
(require '[langohr.basic :as lhb])

(let [[metadata payload] (lhb/get channel queue)]
  (println metadata)
  (println (String. ^bytes payload)))
```


## Changes between Langohr 1.0.0-beta6 and 1.0.0-beta7

`1.0.0-beta7` has **BREAKING CHANGES**:

### langohr.basic/consume Handler Names

The options `langohr.consumers/create-default` takes now have consistent naming:

 * `:consume-ok-fn` becomes `:handle-consume-ok-fn`
 * `:cancel-fn` becomes `:handle-cancel-fn`
 * `:cancel-ok-fn` becomes `:handle-cancel-ok-fn`
 * `:shutdown-signal-ok-fn` becomes `:handle-shutdown-signal-ok-fn`
 * `:recover-ok-fn` becomes `:handle-recover-ok-fn`
 * `:handle-delivery-fn` does not change

This makes handler argument names consistent across the board.


## Changes between Langohr 1.0.0-beta5 and 1.0.0-beta6

`1.0.0-beta6` has **BREAKING CHANGES**:

### langohr.consumes/create-default Delivery Handler Signature Change

`langohr.consumers/create-default`'s `:handle-delivery-fn` signature is now consistent with
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
