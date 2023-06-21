package akka_akka_streams.homework

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.RunnableGraph
import akka_akka_streams.AkkaDataStreams.AkkaStreamGraph.graph


object homeworktemplate {
  implicit val system = ActorSystem("fusion")
  implicit val materializer = ActorMaterializer()
  val graph = ???

  def main(args: Array[String]) : Unit ={
    RunnableGraph.fromGraph(graph).run()

  }
}