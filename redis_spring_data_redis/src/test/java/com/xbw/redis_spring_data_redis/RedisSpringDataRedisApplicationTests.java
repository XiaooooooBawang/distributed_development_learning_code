package com.xbw.redis_spring_data_redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xbw.redis_spring_data_redis.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.Map;

@SpringBootTest
class RedisSpringDataRedisApplicationTests {


	/**
	 * 注入RedisTemplate
	 * value可以放各种类型的数据
	 */
	@Resource
	private RedisTemplate<String, Object> redisTemplate;


	/**
	 * RedisTemplate的子类：StringRedisTemplate，它的key和value的序列化方式默认就是String方式
	 */
	@Resource
	private StringRedisTemplate stringRedisTemplate;

	@Test
	void testString() {
		// 写入一条String数据
		//RedisTemplate可以接收任意Object作为值写入,只不过写入前会把Object序列化为字节形式，
		// 源码中可以看出如果没有设置valueSerializer，默认是采用JDK序列化

		//key也一样，虽然用了泛型，但源码中可以看出如果没有设置keySerializer，默认采用jdk序列化将String序列化
		redisTemplate.opsForValue().set("name","spring_data_redis");
		// 获取string数据
		Object name = redisTemplate.opsForValue().get("name");
		System.out.println(name);
	}

	@Test
	void testSaveUser() {
		User user = new User("xbw", 20);
		redisTemplate.opsForValue().set("user:1", user);
		User o = (User) redisTemplate.opsForValue().get("user:1");
		System.out.println(o);

		/*
		redis中存入的value结果如下，查询时能自动把JSON反序列化为Java对象是因为json对象中还存入了class，但这也带来额外的内存开销
		{
			"@class": "com.xbw.redis_spring_data_redis.domain.User",
			"name": "xbw",
			"age": 20
		}
		*/
	}


	/* -- 下面是用 stringRedisTemplate 来写数据*/

	@Test
	void testString2() {
		stringRedisTemplate.opsForValue().set("name","spring_data_redis");
		Object name = stringRedisTemplate.opsForValue().get("name");
		System.out.println(name);
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void testSaveUser2() throws JsonProcessingException {
		User user = new User("xbw2", 21);
		// 手动序列化
		String json = MAPPER.writeValueAsString(user);
		stringRedisTemplate.opsForValue().set("user:2", json);
		String jsonUser = stringRedisTemplate.opsForValue().get("user:2");
		// 手动反序列化
		User user2 = MAPPER.readValue(jsonUser,User.class);
		System.out.println(user2);

		/*
		redis中存入的value结果如下，没有了class
		{
			"name": "xbw",
			"age": 20
		}
		*/
	}
	@Test
	void testHash() {
		stringRedisTemplate.opsForHash().put("user:3", "name", "xbw3");
		stringRedisTemplate.opsForHash().put("user:3", "age", "23");
		Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries("user:3");
		System.out.println(entries);
	}
}
