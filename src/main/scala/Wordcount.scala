import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.{Materialized, Produced}
import org.apache.kafka.streams.scala.ImplicitConversions._
import java.util.Properties

object Wordcount extends App {
  val props = new Properties

  import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
  import org.apache.kafka.streams.state.KeyValueStore

  import java.util
  import java.util.Locale
  import java.util.concurrent.CountDownLatch

  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-wordcount")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
  props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass)
  props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass)

  val builder = new StreamsBuilder

  val source = builder.stream("streams-plaintext-input")
  source.flatMapValues((value: String) => util.Arrays.asList(value.toLowerCase(Locale.getDefault)
    .split("\\W+"))).groupBy((key, value) => value)
    //FIXME
      .count(Materialized.as[String, Long, KeyValueStore[Nothing, Array[Byte]]]("counts-store"))
    .toStream
    .to("streams-wordcount-output", Produced.`with`(Serdes.String, Serdes.Long))

  val topology = builder.build
  val streams = new KafkaStreams(topology, props)
  val latch = new CountDownLatch(1)

  Runtime.getRuntime.addShutdownHook(new Thread("streams-shutdown-hook") {
    override def run(): Unit = {
      streams.close()
      latch.countDown()
    }
  })
  try {
    streams.start()
    latch.await()
  } catch {
    case e: Throwable =>
      System.exit(1)
  }
  System.exit(0)
}