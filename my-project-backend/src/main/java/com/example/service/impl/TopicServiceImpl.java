package com.example.service.impl;

import com.example.entity.dto.TopicType;
import com.example.mapper.TopicTypeMapper;
import com.example.service.TopicService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TopicServiceImpl implements TopicService {

    @Resource
    TopicTypeMapper mapper;

    @Override
    public List<TopicType> listTypes() {
        return mapper.selectList(null);
    }
}
