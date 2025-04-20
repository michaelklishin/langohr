## Changes between Langohr 5.5.0 and 5.6.0 (in development)

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `5.25.x`.


## Changes between Langohr 5.4.0 and 5.5.0 (October 22, 2024)

### HTTP 2 Client

Contributed by @jimpil.

GitHub issue: [#119](https://github.com/michaelklishin/langohr/pull/119)

### Support for More (Java Client's) ConnectionFactory Options

Contributd by @vincentjames501.

GitHub issue: [#122](https://github.com/michaelklishin/langohr/pull/122)

### Default Consumer Had an Incorrect Signature of handleRecoveryOk

Contributed by @jimpil.

GitHub issue: [#116](https://github.com/michaelklishin/langohr/issues/116)

### Correctly Pick TLS Port (5671) When Only :ssl is Passed in Connection Options

Contributed by @vincentjames501.

GitHub issue: [#114](https://github.com/michaelklishin/langohr/issues/114)

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `5.22.x`.


## Changes between Langohr 5.3.0 and 5.4.0 (April 24, 2022)

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `5.14.x`.


## Changes between Langohr 5.2.0 and 5.3.0 (December 13, 2021)

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `5.13.x`.


## Changes between Langohr 5.1.0 and 5.2.0 (August 11, 2020)

### Support for Overriding of CLient Properties and Client-Provided Connection Name

Contributed by Glen Mailer.

GitHub issue: [#107](https://github.com/michaelklishin/langohr/pull/107)

### A Way to Close All Connections

`langohr.http/close-all-connections` is a new function that closes all client
connections on the target nodes. This is primarily useful in integration tests
and certain monitoring scenarios.

### Corrected Arity of `langohr.http/get-node`

Correct a typo in `langohr.http/get-node` that made the "short
arity" version fail.

GitHub issue: #101.

### Hostname Verification Support

`langohr.core/connect` now support hostname verification via the new `:verify-hostname`
option (a boolean). Hostname verification is one part of [TLS peer verification](https://www.rabbitmq.com/ssl.html#peer-verification)
supported by RabbitMQ Java client and now Langohr.

GitHub issue: #100.

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `5.9.x`.



## Changes between Langohr 5.0.0 and 5.1.0 (January 14th, 2019)

### Clojure Dependency Update

Langohr now depends on Clojure `1.10.0` by default.

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `5.5.x`.


## Changes between Langohr 4.2.0 and 5.0.0 (March 21st, 2018)

This release includes **breaking public API changes**.

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `5.x`.

### JDK 8 is Now Required

RabbitMQ Java client 5.x requires JDK 8. It's a good chance
to drop support for older JDKs in Langohr. Langohr `4.x` continues
to use a JDK 6 and 7-compatible version of the Java client.

### Queueing/Blocking Consumers are Removed

RabbitMQ Java client 5.0 removed a long deprecated queueing consumer
abstraction that used an internal `j.u.c` queue for deliveries and acted as
an iterator. That consumer implementation never supported automatic connection
recovery and isn't necessary with modern consumer operation dispatch pool.

Langohr follows suit and removes the following functions based on the `QueueingConsumer`:

 * `langohr.basic/blocking-subscribe`
 * `langohr.consumers/create-queueing`
 * `langohr.consumers/deliveries-seq`

`langohr.consumers/deliveries-seq` may be reintroduced in the future if a reasonable
imlementation for it comes to mind/is contributed.

### clj-http Upgrade

clj-http dependency has been updated to `3.8.x`.


## Changes between Langohr 4.1.0 and 4.2.0 (December 27th, 2017)

### Type Hinting Improvements

Deleted a redundant type hint.

Contributed by Michal Masztalski.


### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `4.4.1`.

### clj-http Upgrade

clj-http dependency has been updated to `3.7.0`.

### Cheshire Upgrade

Cheshire dependency has been updated to `5.8.0`.




## Changes between Langohr 4.0.0 and 4.1.0 (July 23rd, 2017)

### clj-http Upgrade

clj-http dependency has been updated to `3.6.0`.

### Cheshire Upgrade

Cheshire dependency has been updated to `5.7.1`.




## Changes between Langohr 3.7.0 and 4.0.0 (May 15th, 2017)

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `4.0.0`.

### clj-http Upgrade

clj-http dependency has been updated to `3.5.0`.



## Changes between Langohr 3.6.1 and 3.7.0 (December 9th, 2016)

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.6.6`.

### clj-http Upgrade

clj-http dependency has been updated to `3.4.1`.

### Cheshire Upgrade

Cheshire dependency has been updated to `5.6.3`.



## Changes between Langohr 3.5.x and 3.6.1 (June 18th, 2016)

### Client-Provided Connection Name

`:connection-name` is a new connection option supported by Langohr `3.6.0` and RabbitMQ
server `3.6.2` or later. It can be used to set a client- or application-specific
connection name that will be displayed in the management UI.

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.6.2`.

### clj-http Upgrade

clj-http dependency has been updated to `3.1.0`.

### Cheshire Upgrade

Cheshire dependency has been updated to `5.6.1`.


## Changes between Langohr 3.5.x and 3.5.1 (Feb 5th, 2016)

### API reference corrections.

GH issue: [#79](https://github.com/michaelklishin/langohr/issues/79).



## Changes between Langohr 3.4.x and 3.5.0 (Jan 13th, 2016)

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.6.0`.



## Changes between Langohr 3.4.1 and 3.4.2

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.5.7`.



## Changes between Langohr 3.3.x and 3.4.0

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.5.6`.


## Changes between Langohr 3.2.x and 3.3.0

### Forgiving Exception Handler by Default

Langohr now uses Java client's [forgiving exception handler](https://github.com/rabbitmq/rabbitmq-server/releases/tag/rabbitmq_v3_5_4)
by default. This means unhandled consumer exceptions
won't result in channel closure.


### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.5.4`.

### clj-http Upgrade

clj-http dependency has been updated to `2.0.0`.

This version of clj-http bumps Apache HTTP client version to 4.5.
If this is undesirable for your project, you can exclude Langohr's
dependency on clj-http and use another version.

See Langohr's `project.clj` (the `cljhttp076` profile).

### Cheshire Upgrade

Cheshire dependency has been updated to `5.5.0`.



## Changes between Langohr 3.1.x and 3.2.0

### Authentication Mechanism Support

Langohr now converts `:authentication-mechanism` option to a SASL
mechanism. Two values are supported:

 * `"PLAIN"`
 * `"EXTERNAL"`

Contributed by Tap.

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.5.2`.

### clj-http Upgrade

clj-http dependency has been updated to `1.1.1`.


## Changes between Langohr 3.0.0 and 3.1.0

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.4.4`.

It includes an important binding recovery bug fix.

### clj-http Upgrade

clj-http dependency has been updated to `1.0.1`.

### Cheshire Upgrade

Cheshire dependency has been updated to `5.4.0`.


### langohr.consumers/blocking-subscribe No Longer Fails

`langohr.consumers/blocking-subscribe` no longer fails with a function arity
exception.

GH issue: [#65](https://github.com/michaelklishin/langohr/issues/65).




## Changes between Langohr 2.11.x and 3.0.0

### Options as Maps

Functions that take options now require a proper Clojure map instead of
pseudo keyword arguments:

``` clojure
;; in Langohr 2.x

(lq/declare ch q :durable true)
(lhcons/subscribe ch q (fn [_ _ _])
                        :consumer-tag ctag :handle-cancel-ok (fn [_]))
(lb/publish ch "" q "a message" :mandatory true)

;; in Langohr 3.x
(lq/declare ch q {:durable true})
(lhcons/subscribe ch q (fn [_ _ _])
                        {:consumer-tag ctag :handle-cancel-ok (fn [_])})
(lb/publish ch "" q "a message" {:mandatory true})
```


### JDK 8 Compatibility

Langohr test suite now passes on JDK 8 (previously there was 1 failure
in recovery test).

GH issue: [#54](https://github.com/michaelklishin/langohr/issues/54).


### Connection Recovery Performed by Java Client

Langohr no longer implements automatic connection recovery
of its own. The feature is still there and there should be no
behaviour changes but the functionality has now been pushed
"upstream" in the Java client, so Langohr now relies on it
to do all the work.

There is two public API changes:

 * `com.novemberain.langohr.Recoverable` is gone, `langohr.core/on-recovery`
   now uses `com.rabbitmq.client.Recoverable` instead in its signature.

 * Server-named queues will change after recovery. Use `langohr.core/on-queue-recovery`
   to register a listener for queue name change.

GH issue: [#58](https://github.com/michaelklishin/langohr/issues/58).


### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.4.2`.

### Custom Exception Handlers

`langohr.core/exception-handler` is a function that customizes
default exception handler RabbitMQ Java client uses:

``` clojure
(require '[langohr.core :as rmq])

(let [(rmq/exception-handler :handle-consumer-exception-fn (fn [ch ex consumer
                                                               consumer-tag method-name]
                                                             ))]
  )
```

Valid keys are:

 * `:handle-connection-exception-fn`
 * `:handle-return-listener-exception-fn`
 * `:handle-flow-listener-exception-fn`
 * `:handle-confirm-listener-exception-fn`
 * `:handle-blocked-listener-exception-fn`
 * `:handle-consumer-exception-fn`
 * `:handle-connection-recovery-exception-fn`
 * `:handle-channel-recovery-exception-fn`
 * `:handle-topology-recovery-exception-fn`

GH issue: [#47](https://github.com/michaelklishin/langohr/issues/47).


### Clojure 1.7 Support

Clojure 1.7-specific compilation issues and warnings were eliminated.

### clj-http Upgrade

clj-http dependency has been updated to `1.0.0`.

### ClojureWerkz Support Upgrade

`clojurewerkz/support` dependency has been updated to `1.1.0`.


### langohr.core/version is Removed

`langohr.core/version` was removed.


## Changes between Langohr 2.10.x and 2.11.0

### Multi-Host Support In langohr.core/connect

`langohr.core/connect` now supports `:hosts` as well as `:host`.
The hosts provided will be iterated over, the first reachable host
will be used.

Example:

``` clojure
(require '[langohr.core :as rmq])

(rmq/connect {:hosts #{"192.168.1.2" "192.168.1.3"}})
;; uses port 5688 for both hosts
(rmq/connect {:hosts #{"192.168.1.2" "192.168.1.3"} :port 5688})
;; uses multiple host/port pairs
(rmq/connect {:hosts #{["192.168.1.2" 5688] ["192.168.1.3" 5689]}})
```


## Changes between Langohr 2.9.x and 2.10.0

### Retries for all IOExceptions During Recovery

All IOException subclasses thrown during connection recovery attempts
will now be retried.

Contributed by Paul Bellamy (Xively).

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.3.1`.


## Changes between Langohr 2.8.x and 2.9.0

### Configurable Default and Per-Operation Options in HTTP API Client

Most HTTP API client functions now have an additional optional arguments,
which is a map of options passed to `clj-http` functions. This lets you fine
tune certain HTTP requests as needed.

In addition, `langohr.http/connect!` now accepts one more argument which serves
as default HTTP client options merged with the options provided per `langohr.http`
function call:

``` clojure
(require '[langohr.http :as hc])

;; non-20x/30x statuses will now throw exceptions
(hc/connect! "http://127.0.0.1:15673" "guest" "guest" {:throw-exceptions true})

;; disable throwing exceptions for an individual operation,
;; because 404 is an expected HTTP response in this case
(hc/vhost-exists? "myapp-production" {:throw-exceptions false})
;= false

;; disabling peer verification for HTTPS requests
(hc/connect! "http://127.0.0.1:15673" "guest" "guest" {:insecure? true})
```

### Thread Factory Customization

It is now possible to customize a `java.util.concurrent.ThreadFactory`
used by Langohr connections. The factory will be used to instantiate
all threads created by the client under the hood.

The primary use case for this is running on Google App Engine which
prohibits direct thread instantiation and requires apps to use
thread manager (or thread factory) from GAE SDK instead.

To provide a custom thread factory, pass it as `:thread-factory` to
`langohr.core/connect`. To reify a thread factory with a Clojure function,
use `langohr.core/thread-factory-from`:

``` clojure
(require '[langohr.core :as lc])

(let [tf (lc/thread-factory-from
            (fn [^Runnable r]
              (Thread. r)))]
  (lc/connect {:thread-factory tf}))
```

### com.rabbitmq.client.TopologyRecoveryException is Used

Langohr now uses com.rabbitmq.client.TopologyRecoveryException instead of
reinventing its own exception to indicate topology recovery failure.

### RabbitMQ Java Client Compatibility

A few RabbitMQ Java client interface compatibility issues are resolved.


## Changes between Langohr 2.7.x and 2.8.0

### Client-side Channel Flow Removed

`langohr.channel/flow` and `langohr.channel/flow?` were removed.
Client-side flow control has been deprecated for a while and was removed
in RabbitMQ Java client 3.3.0.

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.3.0`.

### Clojure 1.6 By Default

Langohr now depends on `org.clojure/clojure` version `1.6.0`. It is
still compatible with Clojure 1.4 and if your `project.clj` depends on
a different version, it will be used, but 1.6 is the default now.

We encourage all users to upgrade to 1.6, it is a drop-in replacement
for the majority of projects out there.

### langohr.http/protocol-ports

`langohr.http/protocol-ports` is a new function that returns
a map of protocol names to protocol ports. The protocols
are listed with `langohr.http/list-enabled-protocols`.


## Changes between Langohr 2.6.x and 2.7.1

### langohr.http/list-enabled-protocols

`langohr.http/list-enabled-protocols` is a new function that lists
the protocols a RabbitMQ installation supports, e.g. `"amqp"` or `"mqtt"`.
Note that this currently does not include WebSTOMP (due to certain technical decisions
in RabbitMQ Web STOMP plugin).


## Changes between Langohr 2.5.x and 2.6.0

### langohr.http/list-connections-from, /close-connections-from

`langohr.http/list-connections-from` and `langohr.http/close-connections-from`
are two new functions that list and close connections for a given username,
respectively:

``` clojure
(require '[langohr.http :as hc])

(hc/list-connections-from "guest")
;= a list of connections with username "guest"

;; closes all connections from "guest"
(hc/close-connections-from "guest")
```

### clj-http Upgrade

clj-http dependency has been updated to `0.9.0`.


## Changes between Langohr 2.3.x and 2.5.0

### langohr.http/declare-user Renamed

`langohr.http/declare-user` was renamed to `langohr.http/set-user`.

### langohr.http/declare-policy Renamed

`langohr.http/declare-policy` was renamed to `langohr.http/set-policy`.

### langohr.http/declare-permissions Renamed

`langohr.http/declare-permissions` was renamed to `langohr.http/set-permissions`.

### langohr.http/declare-user Renamed

`langohr.http/declare-user` was renamed to `langohr.http/add-user`.

### langohr.http/vhost-exists?

`langohr.http/vhost-exists?` is a new function that returns true if provided
vhost exists:

``` clojure
(require '[langohr.http :as hc])

(hc/vhost-exists? "killer-app-dev")
```

### langohr.http/user-exists?

`langohr.http/user-exists?` is a new function that returns true if provided
user exists:

``` clojure
(require '[langohr.http :as hc])

(hc/user-exists? "monitoring")
```

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.2.4`.

### clj-http Upgrade

clj-http dependency has been updated to `0.7.9`.

### Topology Recovery Default

`:automatically-recover-topology` default is now `true`, as listed in
documentation.

Contributed by Ilya Ivanov.

### Deprecations

`langohr.core/automatically-recover?` is deprecated

Use `langohr.core/automatic-recovery-enabled?` instead.


## Changes between Langohr 2.2.0 and 2.3.0

### Recovery Predicates

`langohr.core/automatic-recovery-enabled?` and `langohr.core/automatic-topology-recovery-enabled?`
are new predicate functions that return `true` if automatic connection and topology recovery,
respectively, is enabled for the provided connection.

### Topology Recovery Fails Quickly

Topology recovery now fails quickly, raising
`com.novemberain.langohr.recovery.TopologyRecoveryException` which
carries the original (cause) exception.

Previously if recovery of an entity failed, other entities were still
recovered. Now topology recovery fails on the first exception,
making issues more visible.

### Automatic Recovery Can Be Disabled By Passing `nil`

Automatic recovery options now respect both `false` and `nil` values.

### Automatic Topology Recovery Doesn't Kick In When Disabled

Automatic topology recovery no longer kicks in when it is disabled.


## Changes between Langohr 2.1.0 and 2.2.0

### Automatic Topology Recovery Tracks Entities Per Connection

Automatic topology recovery now tracks entities (exchanges,
queues, bindings, and consumers) per connection. This makes
it possible to, say, declare an exchange on one channel, delete
it on another channel and not have it reappear.

Suggested by Jonathan Halterman.


### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.2.2`.

### clj-http Upgrade

clj-http dependency has been updated to `0.7.8`.

### Cheshire Upgrade

Cheshire dependency has been updated to `5.3.1`.



## Changes between Langohr 2.0.0 and 2.1.0

### Full Channel State Recovery

Channel recovery now involves recovery of publisher confirms and
transaction modes.

### No Zombie Bindings After Recovery

Langohr now correctly removes bindings from the list of
bindings to recover when a binding is removed using `queue.unbind`
or `exchange.unbind`.


## Changes between Langohr 1.7.0 and 2.0.0

### Topology (Queues, Exchanges, Bindings, Consumers) Recovery

Connection recovery now supports entity recovery. Queues, exchanges,
bindings and consumers can be recovered automatically after channel
recovery. This feature is enabled by default and can be disabled
by setting the `:automatically-recover-topology` option to `false`.

### :requested-channel-max Connection Option

`:requested-channel-max` is a new option accepted by
`langohr.core/connect` that configures how many channels
this connection may have. The limit is enforced on the client
side. `0` means "no limit" and is the default.

Contributed by Glophindale.

### langohr.queue/empty?

`langohr.queue/empty?` is a new function that returns true if provided
queue is empty (has 0 messages ready):

``` clojure
(require '[langohr.queue :as lq])

(lq/empty? ch "a.queue")
;= true
```


### langohr.core/add-shutdown-listener

`langohr.core/add-shutdown-listener` is a helper function that
reifies and registers a shutdown signal listener on a connection.

### langohr.core/add-blocked-listener

`langohr.core/add-blocked-listener` is a helper function that
reifies and registers a `connection.blocked` and `connection.unblocked`
listener on a connection.



## Changes between Langohr 1.6.0 and 1.7.0

### Retries for Connection Recovery

Langohr will now make sure to handle network I/O-related exceptions
during recovery and reconnect every N seconds.

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.2.1`.


## Changes between Langohr 1.5.0 and 1.6.0

### RabbitMQ Java Client Upgrade

RabbitMQ Java client dependency has been updated to `3.2.0`.


### Automatic Recovery Improvements

Connections will only be recovered if shutdown was not application-initiated.

Contributed by Stephen Dienst.


### Support Update

Langohr now depends on ClojureWerkz Support `0.20.0`.

`langohr.conversion/BytePayload` and `langohr.conversion/to-bytes`
are replaced by `clojurewerkz.support.bytes/ByteSource` and
`clojurewerkz.support.bytes/to-byte-array`, respectively.


## Changes between Langohr 1.4.0 and 1.5.0

### Automatic Recovery Improvements

Automatic recovery of channels that are created without an explicit
number now works correctly.

Contributed by Joe Freeman.


### clj-http Upgrade

clj-http dependency has been updated to `0.7.6`.


### Clojure 1.3 is No Longer Supported

Langohr requires Clojure 1.4+ as of this version.


### More Convenient Publisher Confirms Support

`langohr.confirm/wait-for-confirms` is a new function that
waits until all outstanding confirms for messages
published on the given channel arrive. It optionally
takes a timeout:

``` clojure
(langohr.confirm/wait-for-confirms ch)
;; wait up to 200 milliseconds
(langohr.confirm/wait-for-confirms ch 200)
```


### Automatic Recovery Fix

Automatic recovery now can be enabled without causing an exception.


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
