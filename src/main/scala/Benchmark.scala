import org.openjdk.jmh.annotations._
import org.openjdk.jmh.runner.Runner
import org.openjdk.jmh.runner.options.OptionsBuilder

import akka.actor.ActorSystem
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl._
import akka.actor.typed.scaladsl.adapter.ClassicActorSystemOps
import akka.stream.OverflowStrategy
import akka.stream.scaladsl._
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

package abcxyz {
  @OperationsPerInvocation(100000)
  class BehaviorBenchmark {
    def runForBenchmark(spawner: () => Behavior[Protocol.Set]): Unit = {
      implicit val system = ActorSystem("benchmark")
      implicit val ectx = system.dispatcher
      implicit val scheduler = system.toTyped.scheduler
      implicit val timeout: Timeout = 1.minute

      val n = 100000
  
      val stream =
        Source(1 to n)
          .statefulMap(() => new Array[ActorRef[Protocol.Set]](n / 2))(
            { (actors, elem) =>
              val randomOffset = Random.nextInt(elem) % (actors.length)
              if (actors(randomOffset) == null) {
                actors(randomOffset) = system.spawnAnonymous(spawner())
              }
              actors -> (actors(randomOffset) -> elem)
            }, { _ => None }
          )
          .buffer(10000, OverflowStrategy.backpressure)
          .mapAsync(1) {        // 
            case (actor, elem) =>
              import AskPattern.Askable
              actor.ask[Protocol.Response](replyTo => new Protocol.Set(elem, replyTo))
          }
          .runWith(Sink.ignore)
  
      stream.foreach { _ =>
        system.terminate()
      }
  
      Await.result(system.whenTerminated, Duration.Inf)
    }
  
    @Benchmark
    def benchmarkOnMessage(): Unit =
      runForBenchmark(() => OnMessageImplementation.create)
  
    @Benchmark
    def benchmarkAbstractBehavior(): Unit =
    runForBenchmark(() => Implementation.create)
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
  }
}
