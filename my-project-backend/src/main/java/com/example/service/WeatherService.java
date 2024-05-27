package com.example.service;

import com.example.entity.vo.response.WeatherVO;

public interface WeatherService {
    WeatherVO fetchWeather(double latitude, double longitude);
}
