package cn.zzs.jedis;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Test;

import redis.clients.jedis.Jedis;

/**
 * @ClassName: JedisTest
 * @Description: 测试jedis
 * @author: zzs
 * @date: 2019年9月1日 下午9:28:40
 */
public class JedisTest {
	/**
	 * @Title: testKey
	 * @Description: 测试操作String的方法
	 * @author: zzs
	 * @date: 2019年9月1日 下午9:30:43
	 * @return: void
	 * @throws Exception 
	 */
	@Test
	public void testString() throws Exception {
		//获取Jedis对象
		Jedis jedis = JedisUtils.getJedis();
		String key = "jedis-demo:testString";
		//添加键值对
		jedis.set(key, "test001");
		//获取键值对
		System.out.println(jedis.get(key));
		//删除键值对
		jedis.del(key);
		//判断键值对是否存在
		System.out.println(jedis.exists(key));
		//归还连接
		JedisUtils.returnJedis();
	}
	
	/**
	 * @Title: testList
	 * @Description: 测试操作List的方法
	 * @author: zzs
	 * @date: 2019年9月1日 下午9:30:43
	 * @return: void
	 * @throws Exception 
	 */
	@Test
	public void testList() throws Exception {
		//获取Jedis对象
		Jedis jedis = JedisUtils.getJedis();
		String key = "jedis-demo:testlist5";
		//添加元素
		jedis.lpush(key, "test001","test002","test003","test004");
		//修改元素：只能根据索引删除
		jedis.lset(key, 0, "testList001");
		//删除第一个元素：只能删除第一个或最后一个
		jedis.lpop(key);
		//遍历元素
		List<String> list = jedis.lrange(key, 0, -1);
		Iterator<String> iterator = list.iterator();
		while(iterator.hasNext()) {
			System.out.println(iterator.next());
		}
		//归还连接
		JedisUtils.returnJedis();
	}

	/**
	 * @Title: testSet
	 * @Description: 测试操作Set的方法
	 * @author: zzs
	 * @date: 2019年9月1日 下午9:30:43
	 * @return: void
	 * @throws Exception 
	 */
	@Test
	public void testSet() throws Exception {
		//获取Jedis对象
		Jedis jedis = JedisUtils.getJedis();
		String key = "jedis-demo:testSet";
		//添加元素
		jedis.sadd(key, "test001","test002","test004","test003");
		//更新元素：不能更新元素
		//删除元素
		jedis.srem(key, "test001");
		//判断元素是否存在
		System.out.println(jedis.sismember(key, "test001"));
		//遍历元素
		Set<String> smembers = jedis.smembers(key);
		Iterator<String> iterator = smembers.iterator();
		while(iterator.hasNext()) {
			System.out.println(iterator.next());
		}
		//归还连接
		JedisUtils.returnJedis();
	}
	
	/**
	 * @Title: testSortedSet
	 * @Description: 测试操作Sorted Set的方法
	 * @author: zzs
	 * @date: 2019年9月1日 下午9:30:43
	 * @return: void
	 * @throws Exception 
	 */
	@Test
	public void testSortedSet() throws Exception {
		//获取Jedis对象
		Jedis jedis = JedisUtils.getJedis();
		String key = "jedis-demo:testSortedSet";
		//添加元素
		jedis.zadd(key, 1D, "test001");
		jedis.zadd(key, 2D, "test002");
		jedis.zadd(key, 4D, "test004");
		jedis.zadd(key, 3D, "test003");
		//更新元素
		jedis.zadd(key, 5D, "test001");
		//删除元素
		jedis.zrem(key, "test002");
		//查找指定元素的score
		System.out.println(jedis.zscore(key,"test003"));
		//查找指定指定元素的排名
		System.out.println(jedis.zrank(key,"test003"));
		//遍历元素
		Set<String> smembers = jedis.zrange(key, 0, -1);
		Iterator<String> iterator = smembers.iterator();
		while(iterator.hasNext()) {
			System.out.println(iterator.next());
		}
		//归还连接
		JedisUtils.returnJedis();
	}	
	
	/**
	 * @Title: testHash
	 * @Description: 测试操作Hash的方法
	 * @author: zzs
	 * @date: 2019年9月1日 下午9:30:43
	 * @return: void
	 * @throws Exception 
	 */
	@Test
	public void testHash() throws Exception {
		//获取Jedis对象
		Jedis jedis = JedisUtils.getJedis();
		String key = "jedis-demo:testHash";
		//添加元素
		jedis.hset(key, "id", "1");
		jedis.hset(key, "name", "zzs");
		jedis.hset(key, "age", "18");
		jedis.hset(key, "address", "北京");
		//更新元素
		jedis.hset(key, "age", "19");
		//删除元素
		jedis.hdel(key, "id");
		//获取指定元素的值
		System.out.println(jedis.hget(key, "address"));
		//遍历元素
		Map<String, String> map = jedis.hgetAll(key);
		for(Entry<String, String> entry : map.entrySet()) {
			System.out.println(entry.getKey()+"="+entry.getValue());
		}
		//归还连接
		JedisUtils.returnJedis();
	}	
	
	/**
	 * @Title: testKey
	 * @Description: 测试操作key的方法
	 * @author: zzs
	 * @date: 2019年9月1日 下午9:30:43
	 * @return: void
	 * @throws Exception 
	 */
	@Test
	public void testKey() throws Exception {
		//获取Jedis对象
		Jedis jedis = JedisUtils.getJedis();
		String key = "jedis-demo:testKey";
		//添加键值对
		jedis.set(key, "test001");
		//设置key的过期时间
		jedis.expire(key, 5);
		Thread.sleep(3000);
		//查询key的剩余时间
		System.out.println(jedis.ttl(key));
		//清除key的过期时间
		jedis.persist(key);
		System.out.println(jedis.ttl(key));
		//归还连接
		JedisUtils.returnJedis();
	}
}
