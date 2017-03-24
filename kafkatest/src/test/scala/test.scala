/**
 * Created by PC on 2017/3/24.
 */

import java.util.HashSet
import redis.clients.jedis.{JedisPoolConfig, JedisCluster, HostAndPort, Jedis}
import java.util.HashSet
object test {

  def main(args: Array[String]) {
//    val jedis = new Jedis("localhost")
//    jedis.set("name","sillycat")
//    println(jedis.get("name"))


    val config: JedisPoolConfig = new JedisPoolConfig
//    config.setMaxActive(200)
    config.setMaxIdle(10)
//    config.setMaxWait(2000)
    import redis.clients.jedis.JedisPool
    var pool = new JedisPool(config, "localhost", 6379)
    var jedis = pool.getResource()
    jedis.set("foo", "bar")
    var foobar = jedis.get("foo")
    println(jedis.get("foo"))
    pool.returnResourceObject(jedis)
    pool.destroy()

//    val jedisClusterNodes = new HashSet[HostAndPort]
//    jedisClusterNodes.add(new HostAndPort("127.0.0.1", 6379))
//    val jc = new JedisCluster(jedisClusterNodes)
//
//
//
//    jc.set("age","32")
//    println("name = " + jedis.get("name") + " age = " + jc.get("age"))
  }
}
