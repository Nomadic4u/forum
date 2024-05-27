package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.example.entity.vo.response.WeatherVO;
import com.example.service.WeatherService;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

@Slf4j
@Service
public class WeatherServiceImpl implements WeatherService {

    @Resource
    StringRedisTemplate template;

    @Resource
    RestTemplate rest;

    @Value("${spring.weather.key}")
    String key;

    @Override
    public WeatherVO fetchWeather(double latitude, double longitude) {
        return fetchFromCache(latitude, longitude);
    }

    private WeatherVO fetchFromCache(double latitude, double longitude) {
        JSONObject geo = this.decompressStringToJSON(rest.getForObject(
                "https://geoapi.qweather.com/v2/city/lookup?location="+longitude+","+latitude+"&key="+key, byte[].class));
        if(geo == null) return null;
        JSONObject location = geo.getJSONArray("location").getJSONObject(0);
        int id = location.getInteger("id");
        String key = Const.FORUM_WEATHER_CACHE +id;
        String cache = template.opsForValue().get(key);
        if(cache != null)
            return JSONObject.parseObject(cache).to(WeatherVO.class);
        WeatherVO weather = this.fetchFromAPI(id, location);
        if(weather == null)
            return null;
        template.opsForValue().set(key, JSONObject.from(weather).toJSONString(), 1, TimeUnit.HOURS);
        return weather;
    }

    private WeatherVO fetchFromAPI(int id, JSONObject location) {
        WeatherVO vo = new WeatherVO();
        vo.setLocation(location);
        JSONObject now = this.decompressStringToJSON(rest.getForObject(
                "https://devapi.qweather.com/v7/weather/now?location="+id+"&key="+key, byte[].class));
        if(now == null) return null;
        vo.setNow(now.getJSONObject("now"));
        JSONObject hourly = this.decompressStringToJSON(rest.getForObject(
                "https://devapi.qweather.com/v7/weather/24h?location="+id+"&key="+key, byte[].class));
        if(hourly == null) return null;
        vo.setHourly(new JSONArray(hourly.getJSONArray("hourly").stream().limit(5).toList()));
        return vo;
    }

    private JSONObject decompressStringToJSON(byte[] data) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(data));
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = gis.read(buffer)) != -1)
                stream.write(buffer, 0, bytesRead);
            gis.close();
            stream.close();
        } catch (IOException ignored) {
            return null;
        }
        return JSONObject.parseObject(stream.toString());
    }
}
