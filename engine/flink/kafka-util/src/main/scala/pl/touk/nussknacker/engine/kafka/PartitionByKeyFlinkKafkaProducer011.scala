package pl.touk.nussknacker.engine.kafka

import java.util.{Optional, Properties}

import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer
import org.apache.flink.streaming.connectors.kafka.partitioner.FlinkKafkaPartitioner
import org.apache.flink.streaming.util.serialization.KeyedSerializationSchema

object PartitionByKeyFlinkKafkaProducer011 {

  import scala.collection.JavaConverters._

  def apply[T](kafkaAddress: String,
               topic: String,
               serializationSchema: KeyedSerializationSchema[T],
               kafkaProperties: Option[Map[String, String]] = None): FlinkKafkaProducer[T] = {
    val props = new Properties()
    props.setProperty("bootstrap.servers", kafkaAddress)
    kafkaProperties.map(_.asJava: java.util.Map[_, _]).foreach(props.putAll)
    //we give null as partitioner to use default kafka partition behaviour...
    //default behaviour should partition by key
    new FlinkKafkaProducer[T](
      topic, serializationSchema, props, Optional.empty(): Optional[FlinkKafkaPartitioner[T]]
    )
  }

}