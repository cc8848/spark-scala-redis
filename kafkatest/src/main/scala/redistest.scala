/**
 * Created by PC on 2017/3/24.
 */

import java.util

import redis.clients.jedis.{JedisSentinelPool, JedisCluster}
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import redis.clients.jedis._


object redistest extends Serializable {

    def main(args: Array[String]) {
      val jedisClusterNodes = new util.HashSet[HostAndPort]
      //Jedis Cluster will attempt to discover cluster nodes automatically
      jedisClusterNodes.add(new HostAndPort("127.0.0.1", 6379))
      val jc: JedisCluster = new JedisCluster(jedisClusterNodes)
      println(jc.get("NoticesLists"))
//      val StartMoney=jc.get("NoticesLists")
//      StartMoney.toLong
      val redisHost = "127.0.0.1"
      val redisPort = 6379
      val redisTimeout = 30000
      lazy val pool = new JedisPool(new GenericObjectPoolConfig(), redisHost, redisPort, redisTimeout)
      lazy val hook = new Thread {
        override def run = {
          println("Execute hook thread: " + this)
          pool.destroy()
        }
      }
      sys.addShutdownHook(hook.run)

    }
}
