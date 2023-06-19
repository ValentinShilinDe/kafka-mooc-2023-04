package zio
import zio.CanFail.canFailAmbiguous2
import zio.Cause.Die
import zio.Console.printLine
import zio.stream.ZStream
import zio.stream._
import zio._

import java.io.IOException
import scala.concurrent.TimeoutException
//import scala.sys.process.processInternal.{IOException, URL}

object zioStreamsIntroduction  extends App{
  val emptyStream         : ZStream[Any, Nothing, Nothing]   = ZStream.empty
  val oneIntValueStream   : ZStream[Any, Nothing, Int]       = ZStream.succeed(4)
  val oneListValueStream  : ZStream[Any, Nothing, List[Int]] = ZStream.succeed(List(1, 2, 3))
  val finiteIntStream     : ZStream[Any, Nothing, Int]       = ZStream.range(1, 10)
  val infiniteIntStream   : ZStream[Any, Nothing, Int]       = ZStream.iterate(1)(_ + 1)

  //создает чистый поток из списка переменных значений:
  val stream: ZStream[Any, Nothing, Int] = ZStream(1, 2, 3)
  //Поток, содержащий одно значение Unit:
  val unit: ZStream[Any, Nothing, Unit] = ZStream.unit
  //Поток, который не производит никакого значения или завершается с ошибкой:
  val never: ZStream[Any, Nothing, Nothing] = ZStream.never
  // Принимает начальное значение и итеративно применяет заданную функцию к
  // начальному значению. Начальное значение — это первое значение, созданное потоком, за которым следуют f(init), f(f(init)),...
  val nats: ZStream[Any, Nothing, Int] =
    ZStream.iterate(1)(_ + 1) // 1, 2, 3, ...

  //Поток из диапазона целых чисел [min, max):
  val range: ZStream[Any, Nothing, Int] = ZStream.range(1, 5)

  //chank
  val s1 = ZStream.fromChunk(Chunk(1, 2, 3))
  // Or from multiple Chunks:
  val s2 = ZStream.fromChunks(Chunk(1, 2, 3), Chunk(4, 5, 6))

  val streamSchedule = ZStream(1, 2, 3, 4, 5).schedule(Schedule.spaced(1.second))

  //error handling
  val s3 = ZStream(1, 2, 3) ++ ZStream.fail("Oh! Error!") ++ ZStream(4, 5)
  val s4 = ZStream(7, 8, 9)

  val stream1 = s3.orElse(s4)

  //Recovering from Defects
  val s5 = ZStream(1, 2, 3) ++ ZStream.dieMessage("Oh! Boom!") ++ ZStream(4, 5)
  val s6 = ZStream(7, 8, 9)

  val stream2 = s5.catchAllCause(_ => s6)
  //Recovery from Some Errors
  val s7 = ZStream(1, 2, 3) ++ ZStream.fail("Oh! Error!") ++ ZStream(4, 5)
  val s8 = ZStream(7, 8, 9)
  val stream3 = s7.catchSome {
    case "Oh! Error!" => s8
  }

  val s9 = ZStream(1, 2, 3) ++ ZStream.dieMessage("Oh! Boom!") ++ ZStream(4, 5)
  val s10 = ZStream(7, 8, 9)
  val stream4 = s9.catchSomeCause { case Die(value, _) => s10 }

  //Retry a Failing Stream
  val numbers = ZStream(1, 2, 3) ++
    ZStream
      .fromZIO(
        Console.print("Enter a number: ") *> Console.readLine
          .flatMap(x =>
            x.toIntOption match {
              case Some(value) => ZIO.succeed(value)
              case None        => ZIO.fail("NaN")
            }
          )
      )
      .retry(Schedule.exponential(1.second))

  //timeout
  stream.timeoutFail(new TimeoutException)(10.seconds)

  //Using a Sink
  val sum: UIO[Int] = ZStream(1,2,3).run(ZSink.sum)

  //Using fold
  val s11: ZIO[Any, Nothing, Int] = ZStream(1, 2, 3, 4, 5).runFold(0)(_ + _)
  val s12: ZIO[Any, Nothing, Int] = ZStream.iterate(1)(_ + 1).runFoldWhile(0)(_ <= 5)(_ + _)

  //Using foreach
  ZStream(1, 2, 3).foreach(printLine(_))

  //operations
  val stream5 = ZStream.iterate(0)(_ + 1)
  val s13 = stream.take(5)
  // Output: 0, 1, 2, 3, 4

  val s14 = stream.takeWhile(_ < 5)
  // Output: 0, 1, 2, 3, 4

  val s15 = stream.takeUntil(_ == 5)
  // Output: 0, 1, 2, 3, 4, 5

  val s16 = s3.takeRight(3)
  // Output: 3, 4, 5

  //Mapping
  val intStream: UStream[Int] = ZStream.fromIterable(0 to 100)
  val stringStream: UStream[String] = intStream.map(_.toString)

  //It is similar to mapZIO, but will evaluate effects in parallel.
  // It will emit the results downstream in the original order.
  // The n argument specifies the number of concurrent running effects.
//  def fetchUrl(url: URL): Task[String] = ZIO.succeed(???)
//  def getUrls: Task[List[URL]] = ZIO.succeed(???)

  //mapChunk — Each stream is backed by some Chunks.
  // By using mapChunk we can batch the underlying stream and map every Chunk at once:
  val chunked =
  ZStream
    .fromChunks(Chunk(1, 2, 3), Chunk(4, 5), Chunk(6, 7, 8, 9))

  val stream6 = chunked.mapChunks(x => x.tail)

 // val pages = ZStream.fromIterableZIO(getUrls).mapZIOPar(8)(fetchUrl)

  //Filtering
  val s17 = ZStream.range(1, 11).filter(_ % 2 == 0)
  // Output: 2, 4, 6, 8, 10

  // The `ZStream#withFilter` operator enables us to write filter in for-comprehension style
  val s18 = for {
    i <- ZStream.range(1, 11).take(10)
    if i % 2 == 0
  } yield i
  // Output: 2, 4, 6, 8, 10

  val s19 = ZStream.range(1, 11).filterNot(_ % 2 == 0)

  //Scanning
  val scan = ZStream(1, 2, 3, 4, 5).scan(0)(_ + _)
  // Output: 0, 1, 3, 6, 10
  // Iterations:
  //        =>  0 (initial value)
  //  0 + 1 =>  1
  //  1 + 2 =>  3
  //  3 + 3 =>  6
  //  6 + 4 => 10
  // 10 + 5 => 15

  val fold = ZStream(1, 2, 3, 4, 5).runFold(0)(_ + _)
  // Output: 10 (ZIO effect containing 10)

  //Draining
  //Assume we have an effectful stream, which contains a sequence of effects;
  // sometimes we might want to execute its effect without emitting any element,
  // in these situations to discard the results we should use the ZStream#drain method.
  // It removes all output values from the stream:
  val s20: ZStream[Any, Nothing, Nothing] = ZStream(1, 2, 3, 4, 5).drain
  // Emitted Elements: <empty stream, it doesn't emit any element>

  val s21: ZStream[Any, IOException, Int] =
    ZStream
      .repeatZIO {
        for {
          nextInt <- Random.nextInt
          number = Math.abs(nextInt % 10)
          _ <- Console.printLine(s"random number: $number")
        } yield (number)
      }
      .take(3)
  // Emitted Elements: 1, 4, 7
  // Result of Stream Effect on the Console:
  // random number: 1
  // random number: 4
  // random number: 7

  val s23: ZStream[Any, IOException, Nothing] = s2.drain
  // Emitted Elements: <empty stream, it doesn't emit any element>
  // Result of Stream Effect on the Console:
  // random number: 4
  // random number: 8
  // random number: 2

  val logging = ZStream.fromZIO(
    printLine("Starting to merge with the next stream")
  )
  val stream7 = ZStream(1, 2, 3) ++ logging.drain ++ ZStream(4, 5, 6)
  val stream8 = ZStream(1, 2, 3) ++ logging ++ ZStream(4, 5, 6)

  //Changes The ZStream#changes emits elements that are not equal to the previous element:
  val changes = ZStream(1, 1, 1, 2, 2, 3, 4).changes
  // Output: 1, 2, 3, 4

  //Collecting
  //We can perform filter and map operations in a single step using the ZStream#collect operation:
  val source1 = ZStream(1, 2, 3, 4, 0, 5, 6, 7, 8)

  val s24 = source1.collect { case x if x < 6 => x * 2 }
  // Output: 2, 4, 6, 8, 0, 10

  val s25 = source1.collectWhile { case x if x != 0 => x * 2 }
  // Output: 2, 4, 6, 8

  val source2 = ZStream(Left(1), Right(2), Right(3), Left(4), Right(5))

  val s326 = source2.collectLeft
  // Output: 1, 4

  val s427 = source2.collectWhileLeft
  // Output: 1

  val s28 = source2.collectRight
  // Output: 2, 3, 5

  val s29 = source2.drop(1).collectWhileRight
  // Output: 2, 3

  val s30 = source2.map(_.toOption).collectSome
  // Output: 2, 3, 5

  val s31 = source2.map(_.toOption).collectWhileSome
  // Output: empty stream

  //Zipping
  val s32: UStream[(Int, String)] =
    ZStream(1, 2, 3, 4, 5, 6).zipWith(ZStream("a", "b", "c"))((a, b) => (a, b))

  val s33: UStream[(Int, String)] =
    ZStream(1, 2, 3, 4, 5, 6).zip(ZStream("a", "b", "c"))

}