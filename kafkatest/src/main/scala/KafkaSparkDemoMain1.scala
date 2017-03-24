/**
 * Created by PC on 2017/3/22.
 */

import java.util

import com.google.common.net.HostAndPort
import kafka.serializer.StringDecoder
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.apache.spark.SparkConf
import org.apache.spark.streaming.dstream.InputDStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Duration, StreamingContext}
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.slf4j.LoggerFactory
import redis.clients.jedis.{Jedis, JedisSentinelPool, JedisCluster}
import redis.clients.jedis.Pipeline
import redis.clients.jedis.Response

object KafkaSparkDemoMain1 {
   def main(args: Array[String]) {

     //Redis连接池管理类
//     val jedis = new Jedis("localhost")
     lazy val redisClient = new Jedis("localhost")
//     RedisClient redisClient = new RedisClient(redisServer);//创建redis连接池管理类
     //   * redis客户端
     //   */
//     val jedisClusterNodes = new java.util.HashSet[HostAndPort]()
//     jedisClusterNodes.add(new HostAndPort("192.168.1.100", 6379))
//     jedisClusterNodes.add(new HostAndPort("192.168.1.101", 6379))
//     jedisClusterNodes.add(new HostAndPort("192.168.1.103", 6379))
//     jedisClusterNodes.add(new HostAndPort("192.168.1.104", 6379))
//     jedisClusterNodes.add(new HostAndPort("192.168.1.105", 6379))
//     jedisClusterNodes.add(new HostAndPort("192.168.1.106", 6379))
//     jedisClusterNodes.add(new HostAndPort("192.168.1.107", 6379))
//     jedisClusterNodes.add(new HostAndPort("192.168.1.108", 6379))
//     jedisClusterNodes.add(new HostAndPort("192.168.1.109", 6379))
//     // Redis 操作
//     lazy val jedisCluster = new JedisCluster(jedisClusterNodes)




//     val sentinelClusterName = "mymaster"
//
//     val sentinelServerHostHS = new util.HashSet[String]
//     sentinelServerHostHS.add("127.0.0.1:6379")
////     sentinelServerHostHS.add("192.168.1.104:26379")
////     sentinelServerHostHS.add("192.168.1.105:26379")
////     sentinelServerHostHS.add("192.168.1.106:26379")
////     sentinelServerHostHS.add("192.168.1.107:26379")
////     sentinelServerHostHS.add("192.168.1.108:26379")
//
//     // 如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)，默认值是8。
//     val MAX_ACTIVE = -1;
//     // 控制一个pool最多有多少个状态为idle(空闲的)的jedis实例，默认值也是8。
//     val MAX_IDLE = -1;
//     // 等待可用连接的最大时间，单位毫秒，默认值为-1，表示永不超时。如果超过等待时间，则直接抛出JedisConnectionException；
//     val MAX_WAIT = -1;
//     // 超时时间，0永不超时
//     val TIMEOUT = 0;
//
//     val poolConfig = new GenericObjectPoolConfig
//     poolConfig.setMaxTotal(MAX_ACTIVE)
//     poolConfig.setMaxIdle(MAX_IDLE)
//     poolConfig.setMaxWaitMillis(MAX_WAIT)
//     poolConfig.setTestOnBorrow(true)
//     poolConfig.setTestOnReturn(true)
//
//     def getPoolConfig: GenericObjectPoolConfig = poolConfig
//     lazy val jedisSentinelPool = new JedisSentinelPool("mymaster", sentinelServerHostHS, poolConfig, TIMEOUT)
//     lazy val sentinelConnection = jedisSentinelPool.getResource
//     // Redis 操作……
//     // Redis 操作完毕
//     sentinelConnection.close
//     jedisSentinelPool.destroy


       /**
        * 日志
        */
       val chickHashKey = "app::users::click"
       val log = LoggerFactory.getLogger(this.getClass)
     val sparkConf = new SparkConf().setMaster("local[2]").setAppName("kafka-spark-demo")
     val scc = new StreamingContext(sparkConf, Duration(5000))
     scc.checkpoint(".") // 因为使用到了updateStateByKey,所以必须要设置checkpoint
     val topics = Set("click_events") //我们需要消费的kafka数据的topic
     val kafkaParam = Map(
         "metadata.broker.list" -> "192.168.0.217:9092" // kafka的broker list地址
       )
 //    case class LogStashV1(message:String, path:String, host:String, lineno:Double, `@timestamp`:String)
     val stream: InputDStream[(String, String)] = createStream(scc, kafkaParam, topics)
     stream.map(_._2)

     /**
      * 点击事件，json转case class
      */
     val events = stream.flatMap(line => {
       val data = parse(line._2)
       implicit val formats = DefaultFormats
       val event = data.extract[Event]
       println(event.toString)

       val eventjson = compact(render(data))
       println("json----"+eventjson.toString)
//       redisClient.set(chickHashKey+"1",eventjson.toString)
       redisClient.lpush(chickHashKey+"1",eventjson.toString)
       Some(event)
     })

     /**
      * 构建基于uid和点击数的元组，并对key值用累加方式计算，将结果存入redis
      */
     val userClicks = events.map { event => (event.uid.getOrElse("null"), event.click)
     }.reduceByKey(_ + _)
     userClicks.foreachRDD { rdd => {
       rdd.foreachPartition(partitionOfRecords => {
         partitionOfRecords.foreach(pair => {
           val uid = pair._1
           val clickCount = pair._2

//           redisClient.hincrBy(chickHashKey, uid, clickCount)
//           redisClient.set(chickHashKey+"1",events.toString)
//           redisClient.hincrby(chickHashKey, uid, clickCount)
           println(uid +"---:" + clickCount )
//           rdd.saveAsTextFile("E:/home/LOG.txt")
           log.info(s"$uid:$clickCount")
         }
         )

       })
     }
     }
     val wordCounts = events.map { event => (event.uid.getOrElse("null"), event.click)
     }.reduceByKey(_ + _)
     wordCounts.print()

       scc.start() // 真正启动程序
     scc.awaitTermination() //阻塞等待
   }



   val updateFunc = (currentValues: Seq[Int], preValue: Option[Int]) => {
     val curr = currentValues.sum
     val pre = preValue.getOrElse(0)
     Some(curr + pre)
   }

   /**
    * 创建一个从kafka获取数据的流.
    * @param scc           spark streaming上下文
    * @param kafkaParam    kafka相关配置
    * @param topics        需要消费的topic集合
    * @return
    */
   def createStream(scc: StreamingContext, kafkaParam: Map[String, String], topics: Set[String]) = {
     KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](scc, kafkaParam, topics)
   }
 }
/**
 * 行为
 * @param uid 手机uid
 * @param time 点击时间
 * @param os 操作系统型号
 * @param click 点击次数
 */
case class Event(uid: Option[String], time: Long, os: Option[String], click: Int)