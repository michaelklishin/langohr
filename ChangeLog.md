## Changes between Langohr 1.3.0 and 1.4.0

### Network Recovery Callbacks on Connections and Channels

They can be used to re-declare necessary entities using `langohr.core/on-recovery`:

``` clojure
(langohr.core/on-recovery conn (fn [conn] (comment ...)))

(langohr.core/on-recovery ch   (fn [ch] (comment ...)))
```

Unlike OO clients that represent queues and
exchanges as objects, Langohr cannot be more
aggressive about redeclaring entities during
connection recovery.


## Changes between Langohr 1.2.0 and 1.3.0

## Re-introduce langohr.consumers/create-queueing

The function creates a `QueueingConsumer` instance and is very similar
to `langohr.consumers/create-default` in purpose.

Sometimes combining a queueing consumer with
`langohr.consumers/deliveries-seq` is the best way to express a
problem.

### Rename langoh.consumers/consumers-seq to langoh.consumers/deliveries-seq, make it public

`langoh.consumers/deliveries-seq` is a function that turns a `QueueingConsumer` instance
into a lazy sequence of deliveries.

### Use :executor During Connection Recovery

Connection recovery after network failure will now respect the `:executor`
option.


## Changes between Langohr 1.1.0 and 1.2.0

### Langohr Again Uses RabbitMQ Java Client Interfaces

Langohr's implementation of connection and channel now implements
RabbitMQ Java client's interfaces for connection and channel.



## Changes between Langohr 1.0.0 and 1.1.0

### Extended HTTP API Support

`langohr.http` now provides more complete coverage of the RabbitMQ HTTP API.

Contributed by Steffen Dienst.

### langohr.consumers/subscribe Options In Line with Docs

The documentation says to use function handler keys ending in
"-fn", but this code currently only recognizes the old form. This
commit ensures that all keys that are used within
`langohr.consumers/subscribe` can be used as a parameter.

Contributed by Steffen Dienst.

### langohr.shutdown/sort-error? => langohr.shutdown/soft-error?

`langohr.shutdown/soft-error?` is now correctly named.

Contributed by Ralf Schmitt.

### langohr.core/connect-to-first-available is Removed

`langohr.core/connect-to-first-available` is removed. A better failover functionality
will be available in future versions.

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.1.3`.

### clj-http Upgrade

clj-http dependency has been updated to `0.7.4`.

### Cheshire Upgrade

Cheshire dependency has been updated to `5.2.0`.




## Changes between Langohr 1.0.0-beta13 and 1.0.0

### Queueing Consumers

In its early days, Langohr has been using `QueueingConsumer` for `langohr.queue/subscribe`.
It was later replaced by a `DefaultConsumer` implementation.

The key difference between the two is that

 * `QueueingConsumer` blocks the caller
 * with `QueueingConsumer`, deliveries are typically processed in the same thread

This implementation has pros and cons. As such, an implementation on top of
`QueueingConsumer` is back with `langohr.consumers/blocking-subscribe` which is
identical to `langohr.consumers/subscribe` in the signature but blocks the caller.

In addition, `langohr.consumers/ack-unless-exception` is a new convenience function
that takes a delivery handler fn and will return a new function
that explicitly acks deliveries unless an exception was raised by the original handler:

``` clojure
(require '[langohr.consumers :as lc])
(require '[langohr.basic     :as lb])

(let [f  (fn [metadata payload]
           (comment "Message delivery handler"))
      f' (lc/ack-unless-exception f)]
  (lb/consume ch q (lc/create-default :handle-delivery-fn f'))
```

Contributed by Ian Eure.

### Shutdown Signal Functions

Several new functions in `langohr.shutdown` aid with shutdown signals:

 * `langohr.shutdown/initiated-by-application?`
 * `langohr.shutdown/initiated-by-broker?`
 * `langohr.shutdown/reason-of`
 * `langohr.shutdown/channel-of`
 * `langohr.shutdown/connection-of`

### Clojure 1.5 By Default

Langohr now depends on `org.clojure/clojure` version `1.5.0`. It is
still compatible with Clojure 1.3 and if your `project.clj` depends on
a different version, it will be used, but 1.5 is the default now.

We encourage all users to upgrade to 1.5, it is a drop-in replacement
for the majority of projects out there.


## Changes between Langohr 1.0.0-beta12 and 1.0.0-beta13

`1.0.0-beta13` has **BREAKING CHANGES**:

### langohr.consumers/subscribe Options Renamed

The options `langohr.consumers/subscribe` takes now have consistent naming:

 * `:handle-consume-ok` becomes `:handle-consume-ok-fn`
 * `:handle-cancel` becomes `:handle-cancel-fn`
 * `:handle-cancel-ok` becomes `:handle-cancel-ok-fn`
 * `:handle-shutdown-signal-ok` becomes `:handle-shutdown-signal-ok-fn`
 * `:handle-recover-ok` becomes `:handle-recover-ok-fn`
 * `:handle-delivery-fn` does not change

This makes handler argument names consistent across the board.

Previous options (`:handle-cancel`, etc) are still supported
for backwards compatibility but will eventually be removed.

## Changes between Langohr 1.0.0-beta11 and 1.0.0-beta12

### Clojure-friendly Return Values

Previously functions such as `langohr.queue/declare` returned the underlying
RabbitMQ Java client responses. In case a piece of information from the
response was needed (e.g. to get the queue name that was generated by
RabbitMQ), the only way to obtain it was via the Java interop.

This means developers had to learn about how the Java client works.
Such responses are also needlessly unconvenient when inspecting them
in the REPL.

Langohr `1.0.0-beta12` makes this much better by returning a data structure
that behaves like a regular immutable Clojure map but also provides the same
Java interoperability methods for backwards compatibility.

For example, `langohr.queue/declare` now returns a value that is a map
but also provides the same `.getQueue` method you previously had to use.

Since the responses implement all the Clojure map interfaces, it is possible to use
destructuring on them:

``` clojure
(require '[langohr.core  :as lhc])
(require '[langohr.queue :as lhq])

(let [conn    (lhc/connect)
      channel (lhc/create-channel conn)
      {:keys [queue] :as declare-ok} (lhq/declare channel "" :exclusive true)]
  (println "Response: " declare-ok)
  (println (format "Declared a queue named %s" queue)))
```

will output

```
Response:  {:queue amq.gen-G9bmz19UjHLBjyxhanOG3Q, :consumer-count 0, :message_count 0, :consumer_count 0, :message-count 0}
Declared a queue named amq.gen-G9bmz19UjHLBjyxhanOG3Q
```

### langohr.confirm/add-listener Now Returns Channel

`langohr.confirm/add-listener` now returns the channel instead of the listener. This way
it is more useful with the threading macro (`->`) that threads channels (a much more
common use case).

### langohr.exchange/unbind

`langohr.exchage/unbind` was missing in earlier releases and now added.

### langohr.core/closed?

`langohr.core/closed?` is a new function that complements `langohr.core/open?`.

### langohr.queue/declare-server-named

`langohr.queue/declare-server-named` is a new convenience function
that declares a server-named queue and returns the name RabbitMQ
generated:

``` clojure
(require '[langohr.core  :as lhc])
(require '[langohr.queue :as lhq])

(let [conn    (lhc/connect)
      channel (lhc/create-channel conn)
      queue   (lhq/declare-server-named channel)]
  (println (format "Declared a queue named %s" queue))
```

### More Convenient TLS Support

Langohr will now correct the port to TLS/SSL if provided `:port` is
`5672` (default non-TLS port) and `:ssl` is set to `true`.



## Changes between Langohr 1.0.0-beta10 and 1.0.0-beta11

### HTTP API Client

Langohr `1.0.0-beta11` features initial bits of RabbitMQ HTTP API client
under `langohr.http`.


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

If the exchange does exist, the function has no effect. If not,
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
