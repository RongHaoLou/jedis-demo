
# jedis

## 简介   
`jedis`用于创建和管理连接，并提供了对`redis`数据库的操作方法。`Jedis`相当于`JDBC`中的`Connection`,`JedisPool`相当于`DBCP`或者`C3P0`，而`JedisCluster`更像是`Spring`中的`JDBCTemplate`(将连接池和连接都给封装起来了，只提供给我们直接操作redis的api)。  

`JedisPool`和`JedisCluster`在构造连接池对象的时候，需要传入`JedisPoolConfig`，`host`，`port`等。这些配置信息我们可以统一地定义在`jedis.properties`文件中，方便后续维护。但`jedis`有个不足，就是不能读取`properties`文件，需要我们手动取值设置。  

## 使用例子
### 需求
使用jedis连接池对redis进行`String`、`Set`、`Sorted Set`、`List`、`Hash`和`Key`的操作。（这里仅测试单机版，集群不涉及）

### 工程环境
JDK：1.8.0_201  
maven：3.6.1  
IDE：Spring Tool Suites4 for Eclipse  
redis：3.2.100(windows64)  

### 主要步骤
1. 读取`jedis.properties`文件，创建并设置`JedisPoolConfig`；  
2. 根据配置参数构造`JedisPool`；  
3. 调用`JedisPool`的`getResource`方法获取`Jedis`对象；  
4. 使用`Jedis`进行CRUD操作。  

### 创建项目
项目类型`Maven Project`，打包方式`jar`  

### 引入依赖
```xml
<dependency>
	<groupId>junit</groupId>
	<artifactId>junit</artifactId>
	<version>4.12</version>
	<scope>test</scope>
</dependency>
<!-- jedis -->
<dependency>
	<groupId>redis.clients</groupId>
	<artifactId>jedis</artifactId>
	<version>2.9.3</version>
</dependency>
```
### 编写jedis.properties
路径：`resources`目录下
```properties
#############基本参数#############
#ip地址
redis.host=127.0.0.1
#端口号
redis.port=6379
#如果有密码
redis.password=root
#客户端超时时间单位是毫秒 默认是2000
redis.timeout=3000

#############资源数控制参数#############
#资源池的最大连接数。默认值8
redis.pool.maxTotal=8
#资源池允许最大空闲连接数。默认值8。建议=maxTotal
redis.pool.maxIdle=8
#资源池确保最少空闲连接数。默认0。
redis.pool.minIdle=0
#是否开启jmx监控，可用于监控。默认true。建议开启
redis.pool.jmxEnabled=true

#############借还参数#############
#当资源池用尽后，调用者是否要等待。默认值为true。建议默认。只有当为true时，下面的maxWaitMillis才会生效。
redis.pool.blockWhenExhausted=true
#当资源池连接用尽后，调用者的最大等待时间（单位毫秒)。默认-1：表示永不超时。不建议使用默认值。
redis.pool.maxWaitMillis=3000
#向资源池借用连接时是否做连接有效性检查(ping)，无效连接会被移除。默认false。业务量很大时候建议false。
redis.pool.testOnBorrow=true
#向资源池归还连接时是否做连接有效性测试(ping)，无效连接会被移除。默认false。业务量很大时候建议false。
redis.pool.testOnReturn=false

#############空闲资源监控#############
#是否开启空闲资源监测。默认false。建议true。
redis.pool.testWhileIdle=true
#空闲资源的检测周期(单位为毫秒)。默认-1：不检测。建议设置，周期自行选择。
redis.pool.timeBetweenEvictionRunsMillis=30000
#资源池中资源最小空闲时间(单位为毫秒)，达到此值后空闲资源将被移除。
#默认值1000*60*30 = 30分钟。建议默认，或根据自身业务选择。
redis.pool.minEvictableIdleTimeMillis=300000
#做空闲资源检测时，每次的采样数。默认3。
#可根据自身应用连接数进行微调,如果设置为-1，就是对所有连接做空闲监测。
redis.pool.numTestsPerEvictionRun=3
```

### 编写JedisUtils
这个工具类的主要作用是加载配置文件、初始化连接池、获取连接等。  
路径：`cn.zzs.jedis`
```java
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
```

### 编写测试类
路径：test目录下的`cn.zzs.jedis`，分别测试操作`String`、`Set`、`Sorted Set`、`List`、`Hash`和`Key`。  

#### 测试String
```java
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
```
#### 测试List
注意：list类型的本质是双向链表，jedis提供的api中不支持通过指定value来更新和删除元素。另外list的第一位是从0开始，而-1表示最后一位，-2表示倒数第二位。  
```java
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
```

#### 测试Set
注意：set的本质是哈希表，元素是唯一的，jedis提供的api中并不支持按索引或值更新元素。  
```java
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
```

#### 测试Sorted Set
```java
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
```

#### 测试Hash
```java
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
```

#### 测试Key
```java
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
```

> 学习使我快乐！！
