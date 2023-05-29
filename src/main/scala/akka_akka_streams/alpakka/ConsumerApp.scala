package akka_akka_streams.alpakka

import akka.actor.ActorSystem
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.kafka.scaladsl.Consumer
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, Materializer}
import com.typesafe.config.ConfigFactory
import org.apache.kafka.common.serialization.StringDeserializer

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}
import ch.qos.logback.classic.{Level,Logger}
import org.slf4j.LoggerFactory


object ConsumerApp extends App {
  implicit val system: ActorSystem = ActorSystem("consumer-sys")
  implicit val mat: Materializer = ActorMaterializer()
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  LoggerFactory
    .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    .asInstanceOf[Logger]
    .setLevel(Level.ERROR)

  val config = ConfigFactory.load()
  val consumerConfig = config.getConfig("akka.kafka.consumer")
  val consumerSettings = ConsumerSettings(consumerConfig, new StringDeserializer, new StringDeserializer)

  val consume = Consumer
    .plainSource(consumerSettings, Subscriptions.topics("test"))
    .runWith(Sink.foreach(println))

  consume onComplete  {
    case Success(_) => println("Done"); system.terminate()
    case Failure(err) => println(err.toString); system.terminate()
  }
}