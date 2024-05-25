package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.entity.vo.response.WeatherVO;
import com.example.service.WeatherService;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

@Service
public class WeatherServiceImpl implements WeatherService {

    @Resource
    RestTemplate rest;

    @Resource
    StringRedisTemplate template;

    @Value("${spring.weather.key}")
    String key;

    public WeatherVO fetchWeather(double longitude, double latitude) {
        return fetchFromCache(longitude, latitude);
    }

    // 先从缓冲中获取对应城市的天气信息, 如果没有再去调用api去获取
    private WeatherVO fetchFromCache(double longitude, double latitude) {
        // 获取地理位置数据
        JSONObject geo = this.decompressStingToJson(rest.getForObject(
                "https://geoapi.qweather.com/v2/city/lookup?location=" + longitude + "," + latitude + "&key=" + key, byte[].class));
        if (geo == null) return null;
        // 解析地理位置数据, 火毒location对象的第一个元素
        JSONObject location = geo.getJSONArray("location").getJSONObject(0);
        // 获取城市id
        int id = location.getInteger("id");
        // 构建对应的key
        String key = Const.FORUM_WEATHER_CACHE + id;
        String cache = template.opsForValue().get(key);
        if (cache != null)
            return JSONObject.parseObject(cache).to(WeatherVO.class);
        // 如果缓存中没有 就去调用api获取数据
        WeatherVO vo = this.fetchFromAPI(id, location);
        if (vo == null) return null;
        template.opsForValue().set(key, JSONObject.from(vo).toJSONString(), 1, TimeUnit.HOURS);
        return vo;
    }

    // 获取对应城市的天气信息
    private WeatherVO fetchFromAPI(int id, JSONObject location) {
        WeatherVO vo = new WeatherVO();
        vo.setLocation(location);
        // 调用api获取天气信息
        JSONObject now = this.decompressStingToJson(rest.getForObject(
                "https://devapi.qweather.com/v7/weather/now?location=" + id + "&key=" + key, byte[].class));
        if (now == null) return null;
        vo.setNow(now.getJSONObject("now"));
        // 获取未来24小时的天气数据
        JSONObject hourly = this.decompressStingToJson(rest.getForObject(
                "https://devapi.qweather.com/v7/weather/24h?location=" + id + "&key=" + key, byte[].class));
        if (hourly == null) return null;
        vo.setHourly(new JSONArray(hourly.getJSONArray("hourly").stream().limit(5).toList()));
        return vo;
    }

    // 用于解压字节数组, 然后转为JSONObject
    private JSONObject decompressStingToJson(byte[] data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(data));
            byte[] buffer = new byte[1024];
            int read;
            while ((read = gzip.read(buffer)) != -1)
                stream.write(buffer, 0, read);
            gzip.close();
            stream.close();
            return JSONObject.parseObject(stream.toString());
        } catch (IOException e) {
            return null;
        }
    }
}
