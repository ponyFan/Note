package com.test

import com.github.javafaker.Faker
import com.google.common.collect.Lists
import org.apache.commons.lang3.StringUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{Row, SaveMode, SparkSession}
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

/**
 * ./spark-submit
 * --master yarn
 * --deploy-mode client
 * --name test-spark
 *
 * --jars /root/fzl/spark-yarn-jar/spark-streaming-kafka-0-10_2.11-2.4.3.jar,/root/fzl/spark-yarn-jar/kafka-clients-0.10.2.0.jar,/root/fzl/spark-yarn-jar/mysql-connector-java-8.0.13.jar
 * --class com.test.Test
 * /root/fzl/test-spark-1.0-SNAPSHOT.jar test001
 * @author zelei.fan
 * @date 2019/9/19 9:42
 */
object Test {

  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
    val ssc = new StreamingContext(conf, Seconds(5))
    val sc: SparkContext = ssc.sparkContext
    val ss: SparkSession = SparkSession.builder().config(conf).getOrCreate()
    val topics = Array(args(0)).toSet
    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "192.168.9.22:9092,192.168.9.23:9092,192.168.9.24:9092",
        "auto.offset.reset" -> "earliest",
        "key.deserializer" -> classOf[StringDeserializer],
        "value.deserializer" -> classOf[StringDeserializer],
        "group.id" -> "use_a_separate_group_id_for_each_streamvvvvvvvvvvdddddd",
        "spark.streaming.kafka.maxRatePerPartition" -> "1000"
    )
    val stream = KafkaUtils.createDirectStream[String, String](
      ssc,
      PreferConsistent,
      Subscribe[String, String](topics, kafkaParams)
    )

    val schema = StructType(
      List(
        StructField("id", StringType, true),
        StructField("name", StringType, true),
        StructField("address", StringType, true),
        StructField("country", StringType, true),
        StructField("time", StringType, true)
      )
    )
    val dstream1: DStream[Row] = stream.mapPartitions((iterator: Iterator[ConsumerRecord[String, String]]) => {
      for (elem <- iterator) yield {
        val splits = StringUtils.split(elem.value(), ";")
        if(splits.length > 6){
          Row(splits(0), splits(1), splits(3), splits(4), splits(7))
        }else {
          null
        }
      }
    })

    /*val dstream1: DStream[Row] = stream.map(msg => {
      val splits = StringUtils.split(msg.value(), ";")
      if(splits.length > 6){
        Row(splits(0), splits(1), splits(3), splits(4), splits(7))
      }else {
       null
      }
    })*/

    dstream1.filter(_ != null).foreachRDD(value => {
      val df = ss.createDataFrame(value, schema)
      df.show()
      df.write.mode(SaveMode.Append).parquet("hdfs://nist22:9000/test/parquet")
    })
    ssc.start()
    ssc.awaitTermination()
  }

  case class MessageBean(id:String, name:String, address:String, country:String, time:String){

  }
}
