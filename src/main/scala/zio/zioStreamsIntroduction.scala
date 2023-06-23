package zio

import ch.qos.logback.classic.{Level, Logger}
import zio._
import zio.kafka.consumer._
import zio.kafka.producer.{Producer, ProducerSettings}
import zio.kafka.serde._
import zio.stream.ZStream
import org.slf4j.LoggerFactory

object MainApp extends ZIOAppDefault {
  LoggerFactory
    .getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
    .asInstanceOf[Logger]
    .setLevel(Level.ERROR)

  //is a description of a program that, when evaluated, may emit zero or more values of
  // type O, may fail with errors of type E, and uses an environment of type R.
  val producer: ZStream[Producer, Throwable, Nothing] =
  ZStream
    .repeatZIO(Random.nextIntBetween(0, Int.MaxValue))
    .schedule(Schedule.fixed(2.seconds))
    //Maps over elements of the stream with the specified effectful function.
    .mapZIO { random =>
      Producer.produce[Any, Long, String](
        topic = "random",
        key = random % 4,
        value = random.toString,
        keySerializer = Serde.long,
        valueSerializer = Serde.string
      )
    }
    //Converts this stream to a stream that executes its effects but emits no elements.
    .drain

  val consumer: ZStream[Consumer, Throwable, Nothing] =
    Consumer
      .plainStream(Subscription.topics("random"), Serde.long, Serde.string)
      //Adds an effect to consumption of every element of the stream
      .tap(r => Console.printLine(r.value))
      .map(_.offset)
      //Aggregates elements of this stream using the provided sink for as long as the downstream operators on the stream are busy.

      .aggregateAsync(Consumer.offsetBatches)
      //Maps over elements of the stream with the specified effectful function.
      .mapZIO(_.commit)
      .drain

  def producerLayer =
    ZLayer.scoped(
      Producer.make(
        settings = ProducerSettings(List("localhost:29092"))
      )
    )

  def consumerLayer =
    ZLayer.scoped(
      Consumer.make(
        ConsumerSettings(List("localhost:29092")).withGroupId("group")
      )
    )

  override def run =
    producer.merge(consumer)
      .runDrain
      .provide(producerLayer, consumerLayer)

}
