package akka_akka_streams.Akka

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, SpawnProtocol}
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.Props
import akka_akka_streams.Akka.intro_actors.behaviour_factory_methods

import scala.concurrent.Future
import scala.language.{existentials, postfixOps}
import scala.concurrent.duration._
import scala.language.{existentials, postfixOps}


//1.
object AkkaMain{
  def main(args: Array[String]): Unit ={
    val system = ActorSystem[String](behaviour_factory_methods.Echo(), "Echo")
    system ! "Hello"
    Thread.sleep(3000)
    system.terminate()
  }
}

//2. root actor
object AkkaMain2{
  object Supervisor {
    def apply(): Behavior[SpawnProtocol.Command] = Behaviors.setup{ ctx =>
      ctx.log.info(ctx.self.toString)
      SpawnProtocol()
    }
  }

  def main(args: Array[String]) : Unit ={
    implicit val system = ActorSystem[SpawnProtocol.Command](Supervisor(), "Echo")
    implicit val ec = system.executionContext
    implicit val timeout = Timeout(3 seconds)

    val echo: Future[ActorRef[String]] = system.ask(
      SpawnProtocol.Spawn(intro_actors.behaviour_factory_methods.Echo(),
        "Echo", Props.empty, _))
    for (ref <- echo)
      ref ! "Hello from ask"
  }
}

//3. change state
object AkkaMain3{
  object change_Behaviour{
    sealed trait WorkerProtocol
    object WorkerProtocol{
      case object Start extends WorkerProtocol
      case object StandBy extends WorkerProtocol
      case object Stop extends  WorkerProtocol
    }

    import WorkerProtocol._
    def apply(): Behavior[WorkerProtocol] = idle()
    def idle(): Behavior[WorkerProtocol] = Behaviors.setup{ctx =>
      Behaviors.receiveMessage{
        case msg@Start =>
          ctx.log.info(msg.toString())
          workInProgress()
        case msg@StandBy =>
          ctx.log.info(msg.toString())
          idle()
        case msg@Stop =>
          ctx.log.info(msg.toString())
          Behaviors.stopped
      }
    }

    def workInProgress(): Behavior[WorkerProtocol] = Behaviors.setup{ ctx=>
      Behaviors.receiveMessage{
        case msg@Start => Behaviors.unhandled
        case msg@StandBy =>
          ctx.log.info("go to StandBy")
          idle()
        case msg@Stop =>
          ctx.log.info("stopped")
          Behaviors.stopped
      }
    }
  }
}