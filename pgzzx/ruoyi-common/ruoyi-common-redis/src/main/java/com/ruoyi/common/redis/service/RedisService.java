package com.ruoyi.common.redis.service;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

/**
 * spring redis 工具类
 * 
 * @author ruoyi
 **/
@SuppressWarnings(value = { "unchecked", "rawtypes" })
@Component
public class RedisService
{
    @Autowired
    public RedisTemplate redisTemplate;

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value)
    {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     * @param timeout 时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final Long timeout, final TimeUnit timeUnit)
    {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout)
    {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit)
    {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    public long getExpire(final String key)
    {
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(String key)
    {
        return redisTemplate.hasKey(key);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key)
    {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String key)
    {
        return redisTemplate.delete(key);
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    public boolean deleteObject(final Collection collection)
    {
        return redisTemplate.delete(collection) > 0;
    }

    /**
     * 缓存List数据
     *
     * @param key 缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList)
    {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getCacheList(final String key)
    {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 缓存Set
     *
     * @param key 缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet)
    {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext())
        {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key)
    {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap)
    {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    public <T> Map<String, T> getCacheMap(final String key)
    {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 往Hash中存入数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value)
    {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash中的数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey)
    {
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys)
    {
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * 删除Hash中的某条数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return 是否成功
     */
    public boolean deleteCacheMapValue(final String key, final String hKey)
    {
        return redisTemplate.opsForHash().delete(key, hKey) > 0;
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern)
    {
        return redisTemplate.keys(pattern);
    }
}


//1、set(K key, V value)
//
//        新增一个字符串类型的值，key是键，value是值。
//
//        redisTemplate.opsForValue().set("stringValue","bbb");
//        2、get(Object key)
//        获取key键对应的值。
//
//        String stringValue = redisTemplate.opsForValue().get("key")
//        3、append(K key, String value)
//        在原有的值基础上新增字符串到末尾。
//
//        redisTemplate.opsForValue().append("key", "appendValue");
//        String stringValueAppend = redisTemplate.opsForValue().get("key");
//        System.out.println("通过append(K key, String value)方法修改后的字符串:"+stringValueAppend);
//        4、get(K key, long start, long end)
//        截取key键对应值得字符串，从开始下标位置开始到结束下标的位置(包含结束下标)的字符串。
//
//        String cutString = redisTemplate.opsForValue().get("key", 0, 3);
//        System.out.println("通过get(K key, long start, long end)方法获取截取的字符串:"+cutString);
//        5、getAndSet(K key, V value)
//        获取原来key键对应的值并重新赋新值。
//
//
//        String oldAndNewStringValue = redisTemplate.opsForValue().getAndSet("key", "ccc");
//        System.out.print("通过getAndSet(K key, V value)方法获取原来的值：" + oldAndNewStringValue );
//        String newStringValue = redisTemplate.opsForValue().get("key");
//        System.out.println("修改过后的值:"+newStringValue);
//        6、setBit(K key, long offset, boolean value)
//        key键对应的值value对应的ascii码,在offset的位置(从左向右数)变为value。
//
//        redisTemplate.opsForValue().setBit("key",1,false);
//        newStringValue = redisTemplate.opsForValue().get("key")+"";
//        System.out.println("通过setBit(K key,long offset,boolean value)方法修改过后的值:"+newStringValue);
//        7、getBit(K key, long offset)
//        判断指定的位置ASCII码的bit位是否为1。
//
//        boolean bitBoolean = redisTemplate.opsForValue().getBit("key",1);
//        System.out.println("通过getBit(K key,long offset)方法判断指定bit位的值是:" + bitBoolean);
//        8、size(K key)
//        获取指定字符串的长度
//
//        Long stringValueLength = redisTemplate.opsForValue().size("key");
//        System.out.println("通过size(K key)方法获取字符串的长度:"+stringValueLength);
//        9、increment(K key, double delta)
//        以增量的方式将double值存储在变量中。
//
//        double stringValueDouble = redisTemplate.opsForValue().increment("doubleKey",5);
//        System.out.println("通过increment(K key, double delta)方法以增量方式存储double值:" + stringValueDouble);
//        10、increment(K key, long delta)
//        以增量的方式将long值存储在变量中。
//
//        double stringValueLong = redisTemplate.opsForValue().increment("longKey",6);
//        System.out.println("通过increment(K key, long delta)方法以增量方式存储long值:" + stringValueLong);
//        11、setIfAbsent(K key, V value)
//        如果键不存在则新增,存在则不改变已经有的值。
//        boolean absentBoolean = redisTemplate.opsForValue().setIfAbsent("absentKey","fff");
//        System.out.println("通过setIfAbsent(K key, V value)方法判断变量值absentValue是否存在:" + absentBoolean);
//        if(absentBoolean){
//        String absentValue = redisTemplate.opsForValue().get("absentKey")+"";
//        System.out.print(",不存在，则新增后的值是:"+absentValue);
//        boolean existBoolean = redisTemplate.opsForValue().setIfAbsent("absentKey","eee");
//        System.out.print(",再次调用setIfAbsent(K key, V value)判断absentValue是否存在并重新赋值:" + existBoolean);
//        if(!existBoolean){
//        absentValue = redisTemplate.opsForValue().get("absentKey")+"";
//        System.out.print("如果存在,则重新赋值后的absentValue变量的值是:" + absentValue);
//        12、set(K key, V value, long timeout, TimeUnit unit)
//        设置变量值的过期时间。
//
//        redisTemplate.opsForValue().set("timeOutKey", "timeOut", 5, TimeUnit.SECONDS);
//        String timeOutValue = redisTemplate.opsForValue().get("timeOutKey")+"";
//        System.out.println("通过set(K key, V value, long timeout, TimeUnit unit)方法设置过期时间，
//        过期之前获取的数据:"+timeOutValue);
//        Thread.sleep(5*1000);
//        timeOutValue = redisTemplate.opsForValue().get("timeOutKey")+"";
//        System.out.print(",等待10s过后，获取的值:"+timeOutValue);
//        13、set(K key, V value, long offset)
//        覆盖从指定位置开始的值。
//
//        redisTemplate.opsForValue().set("absentKey","dd",1);
//        String overrideString = redisTemplate.opsForValue().get("absentKey");
//        System.out.println("通过set(K key, V value, long offset)方法覆盖部分的值:"+overrideString);
//        14、multiSet(Map<? extends K,? extends V> map)
//        设置map集合到redis。
//
//        Map valueMap = new HashMap();
//        valueMap.put("valueMap1","map1");
//        valueMap.put("valueMap2","map2");
//        valueMap.put("valueMap3","map3");
//        redisTemplate.opsForValue().multiSet(valueMap);
//        15、multiGet(Collection<K> keys)
//        根据集合取出对应的value值。
//
//        //根据List集合取出对应的value值
//        List paraList = new ArrayList();
//        paraList.add("valueMap1");
//        paraList.add("valueMap2");
//        paraList.add("valueMap3");
//        List<String> valueList = redisTemplate.opsForValue().multiGet(paraList);
//        for (String value : valueList){
//        System.out.println("通过multiGet(Collection<K> keys)方法获取map值:" + value);
//        }
//        16、multiSetIfAbsent(Map<? extends K,? extends V> map)
//        Map valueMap = new HashMap();
//
//        valueMap.put("valueMap1","map1");
//
//        valueMap.put("valueMap2","map2");
//
//        valueMap.put("valueMap3","map3");
//
//        redisTemplate.opsForValue().multiSetIfAbsent(valueMap);
//
//
//        删除单个key值
//        redisTemplate.delete();