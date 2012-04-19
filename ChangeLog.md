## Changes between Langohr 1.0.0-beta2 and 1.0.0-beta3

No changes yet.


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
