package cn.zzs.jedis;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * @ClassName: JedisUtils
 * @Description: jedis工具类，用于获取连接
 * @author: zzs
 * @date: 2019年9月1日 下午8:47:26
 */
public class JedisUtils {
	private static ThreadLocal<Jedis> tl = new ThreadLocal<Jedis>();
	private static Object obj = new Object();
	//连接池对象
	private static JedisPool jedisPool;
	
	static {
		init();
	}
	/**
	 * @Title: getJedis
	 * @Description: 获取连接对象的方法，线程安全
	 * @author: zzs
	 * @date: 2019年8月31日 下午9:22:29
	 * @return: Jedis
	 */
	public static Jedis getJedis() throws Exception{
		//从当前线程中获取连接对象
		Jedis jedis = tl.get();
		//判断为空的话，创建连接并绑定到当前线程
		if(jedis == null) {
			synchronized (obj) {
				if(tl.get() == null) {
					jedis = jedisPool.getResource();
					tl.set(jedis);
				}
			}
		}
		return jedis;
	}
	/**
	 * @Title: returnJedis
	 * @Description: 交还jedis,校验如果jedis失效就将其移出
	 * @author: zzs
	 * @date: 2019年9月1日 下午9:13:57
	 * @return: void
	 */
	public static void returnJedis() {
		Jedis jedis = tl.get();
		if(jedis!=null) {
			if(!jedis.isConnected()) {
				tl.remove();
				jedis.close();
			}
		}
	}
	
	/**
	 * @Title: init
	 * @Description: 加载配置文件，并初始化jedisPool
	 * @author: zzs
	 * @date: 2019年9月1日 下午8:48:39
	 * @return: void
	 */
	private static void init() {
		//加载配置文件
		InputStream in = JedisUtils.class.getClassLoader().getResourceAsStream("jedis.properties");
		Properties properties = new Properties();
		try {
			properties.load(in);
		} catch (IOException e) {
			System.err.println("加载配置文件失败,创建连接池失败");
			e.printStackTrace();
			return;
		}
		//设置配置对象
		JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
		jedisPoolConfig.setMaxTotal(Integer.parseInt(properties.getProperty("redis.pool.maxTotal")));
		jedisPoolConfig.setMaxIdle(Integer.parseInt(properties.getProperty("redis.pool.maxIdle")));
		jedisPoolConfig.setMinIdle(Integer.parseInt(properties.getProperty("redis.pool.minIdle")));
		jedisPoolConfig.setMaxWaitMillis(Integer.parseInt(properties.getProperty("redis.pool.maxWaitMillis")));
		jedisPoolConfig.setTestOnBorrow(Boolean.parseBoolean(properties.getProperty("redis.pool.testOnBorrow")));
		jedisPoolConfig.setTestOnReturn(Boolean.parseBoolean(properties.getProperty("redis.pool.testOnReturn")));
		//创建jedis连接池
		jedisPool = new JedisPool(jedisPoolConfig,
				properties.getProperty("redis.host"),
				Integer.parseInt(properties.getProperty("redis.port")),
				Integer.parseInt(properties.getProperty("redis.timeout")),
				properties.getProperty("redis.password")
				);
	}
}
