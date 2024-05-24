package com.example.utils;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class CacheUtils {
    @Resource
    StringRedisTemplate template;

    // 缓存中获取单个对象
    public <T> T takeFromCache(String key, Class<T> dataType) {
        String s = template.opsForValue().get(key);
        if(s == null) return null;
        return JSONObject.parseObject(s).to(dataType); // 先将json转为对象, 然后转为指定的类型
    }

    // 缓存中获取列表对象
    public <T> List<T> takeListFromCache(String key, Class<T> itemType) {
        String s = template.opsForValue().get(key);
        if(s == null) return null;
        return JSONArray.parseArray(s).toList(itemType);
    }

    // 保存单个对象
    public <T> void saveToCache(String key, T data, long expire) {
        template.opsForValue().set(key, JSONObject.from(data).toJSONString(), expire, TimeUnit.SECONDS);
    }

    // 保存列表对象
    public <T> void saveListToCache(String key, List<T> list, long expire) {
        template.opsForValue().set(key, JSONArray.from(list).toJSONString(), expire, TimeUnit.SECONDS);
    }

    // 删除匹配模式的缓存 这里是指redis的匹配模式
    public void deleteCachePattern(String keypattern){
        Set<String> keys = Optional.ofNullable(template.keys(keypattern)).orElse(Collections.emptySet());
        template.delete(keys);
    }

    public void deleteCache(String key){
        template.delete(key);
    }
}
