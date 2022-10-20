import org.openjdk.jmh.annotations._
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder

import akka.actor.ActorSystem
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl._
import akka.actor.typed.scaladsl.adapter.{ ClassicActorSystemOps, TypedActorRefOps }
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._

import java.util.concurrent.TimeUnit
import java.util.concurrent.ThreadLocalRandom

package abcxyz {
  @State(Scope.Benchmark)
  @OutputTimeUnit(TimeUnit.MICROSECONDS)
  class BehaviorBenchmark {
    def runForBenchmark(behavior: () => Behavior[Protocol.Command], numMessages: Int): Unit = {
      import BehaviorBenchmark.{ n, spawnActors, system, timeout }

      implicit val ectx = system.dispatcher
      implicit val scheduler = system.toTyped.scheduler

      val actor = system.spawnAnonymous(behavior())

      val stream =
        Source(1 to numMessages)
          .map { elem =>
            val askRequest: ActorRef[Protocol.Response] => Protocol.Command =
              if (elem % 2 == 1) { replyTo => new Protocol.Set(elem, replyTo) }
              else { replyTo => new Protocol.Get(replyTo) }

            askRequest
          }
          .buffer(100, OverflowStrategy.backpressure)
          .mapAsync(1) { request =>
            import AskPattern._
  
            actor.ask(request)
          }
          .runWith(Sink.ignore)

      Await.result(stream, Duration.Inf)

      system.stop(actor.toClassic)
    }

    def benchmarkOnMessage(numMessages: Int): Unit =
      runForBenchmark(() => OnMessageImplementation.create, numMessages)
  
    def benchmarkAbstractBehavior(numMessages: Int): Unit =
      runForBenchmark(() => Implementation.create, numMessages)

    @Benchmark
    def benchmarkOnMessage000(): Unit =
      benchmarkOnMessage(0)

    @Benchmark
    def benchmarkOnMessage001(): Unit =
      benchmarkOnMessage(1)

    @Benchmark
    def benchmarkOnMessage002(): Unit =
      benchmarkOnMessage(2)

    @Benchmark
    def benchmarkOnMessage004(): Unit =
      benchmarkOnMessage(4)

    @Benchmark
    def benchmarkOnMessage008(): Unit =
      benchmarkOnMessage(8)

    @Benchmark
    def benchmarkOnMessage016(): Unit =
      benchmarkOnMessage(16)

    @Benchmark
    def benchmarkOnMessage032(): Unit =
      benchmarkOnMessage(32)

    @Benchmark
    def benchmarkOnMessage064(): Unit =
      benchmarkOnMessage(64)

    @Benchmark
    def benchmarkOnMessage128(): Unit =
      benchmarkOnMessage(128)

    @Benchmark
    def benchmarkAbstractBehavior000(): Unit =
      benchmarkAbstractBehavior(0)

    @Benchmark
    def benchmarkAbstractBehavior001(): Unit =
      benchmarkAbstractBehavior(1)

    @Benchmark
    def benchmarkAbstractBehavior002(): Unit =
      benchmarkAbstractBehavior(2)

    @Benchmark
    def benchmarkAbstractBehavior004(): Unit =
      benchmarkAbstractBehavior(4)

    @Benchmark
    def benchmarkAbstractBehavior008(): Unit =
      benchmarkAbstractBehavior(8)

    @Benchmark
    def benchmarkAbstractBehavior016(): Unit =
      benchmarkAbstractBehavior(16)

    @Benchmark
    def benchmarkAbstractBehavior032(): Unit =
      benchmarkAbstractBehavior(32)

    @Benchmark
    def benchmarkAbstractBehavior064(): Unit =
      benchmarkAbstractBehavior(64)

    @Benchmark
    def benchmarkAbstractBehavior128(): Unit =
      benchmarkAbstractBehavior(128)

    @TearDown(Level.Trial)
    def shutdown(): Unit = {
      BehaviorBenchmark.system.terminate()

      Await.result(BehaviorBenchmark.system.whenTerminated, Duration.Inf)
    }
  }

  object BehaviorBenchmark {
    def main(args: Array[String]): Unit = {
      val options =
        new OptionsBuilder()
          .include(classOf[BehaviorBenchmark].getSimpleName)
          .forks(1)
          .build()
  
      new Runner(options).run()
    }

    final val n = 499999 // should be an odd number
    implicit val timeout: Timeout = 1.minute
    implicit val system = ActorSystem("benchmark")

    def spawnActors(behavior: () => Behavior[Protocol.Command], n: Int): Array[ActorRef[Protocol.Command]] =
      Array.fill(n) { system.spawnAnonymous(behavior()) }
  }
}
