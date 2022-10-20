Akka 2.7.0 introduces a new means of defining behaviors in Java in an OO style: the `AbstractOnMessageBehavior`.  As in the Scala DSL's `AbstractBehavior`,
this approach defines `Behavior<Command>` as a method `onMessage` taking a `Command` and returning a `Behavior<Command>`.  In the Java DSL's pre-existing
`AbstractBehavior`, the message processing logic is represented as a `Receive` object (which is configured in its builder to match incoming messages), while
in the `AbstractOnMessageBehavior` (and `scaladsl.AbstractBehavior`) the `onMessage` method must perform any required matching (e.g. via whatever pattern
matching support one has available) directly.

Scala has long had extensive support for pattern matching, but pattern matching features are a more recent addition (e.g. Java 17, Kotlin) in many other
major JVM languages: the new style may facilitate a more natural behavior definition in those languages.  In addition to the expressivity benefits, it's
hypothesized that `AbstractOnMessageBehavior` is more efficient than the `javadsl.AbstractBehavior`.

In the `AbstractOnMessageBehavior`, the message processing is generated at build time, while in the `javadsl.AbstractBehavior`, a `Receive` is built (using
a builder) on receipt of the first message by an instance of the behavior (it is possible to build the `Receive` when the instance is constructed and save it
in a field to be returned by the `createReceive()` method, though this would only shift this work to when construction happens).  It is hypothesized that
this overhead of building a `Receive` (which is effectively compiling the `instanceof`s and casts one would write in `onMessage`) will result in extra latency
in processing the first message for a behavior.  This repository is an experiment to generate evidence one way or the other.

## Running the benchmark

```
sbt (root)> Jmh / run -i 100 -wi 2 -f1 -t1
```

(100 runs after 2 warm-ups, in a forked JVM)

## Notes about the benchmark

The basic idea is to randomly spawn actors and send messages to them.  Some actors may be sent multiple (or even many) messages, depending on "how the dice fall".
The more messages are sent per run and the more runs, the more this luck should even out.  The hypothesis suggests that the overhead is primarily felt on the
first message, but since the point of an actor is often (with _many_ exceptions!) to receive many messages, there would be nothing more artificial than only
testing first-message latency.

A new `ActorSystem` is constructed and then torn-down per run.  This is a heavyweight operation and should be approximately constant from one run to the next.  The
effect on the benchmark is to reduce the magnitude of reported differences.

## Results

See the `results/` directory.

PRs will be accepted with new results, following the format of the results in that folder.  Feel free to use other settings (number of messages, max. number of actors,
etc.: increasing the max. number of actors relative to the number of messages would be expected to stress first-message latency more) and include them in your results.

### Summary

| Result ID | Relative Advantage/(Disadvantage) | Messages/run, max. actors |
| --------- | --------------------------------- | ------------------------- |
| 20221019-leviramsey | +4.17% | 100k messages, up to 50k actors |
