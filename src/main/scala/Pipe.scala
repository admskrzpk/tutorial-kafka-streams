import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.{KafkaStreams, StreamsBuilder, StreamsConfig}
import java.util.Properties
import java.util.concurrent.CountDownLatch

object Pipe extends App {
  val props = new Properties
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-pipe")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, ":9092")
  props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String.getClass)
  props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String.getClass)

  val builder = new StreamsBuilder
  builder.stream("streams-plaintext-input").to("streams-pipe-output")

  val topology = builder.build
  //System.out.println(topology.describe)

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