package com.example.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Topic;
import com.example.entity.dto.TopicType;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.mapper.TopicMapper;
import com.example.mapper.TopicTypeMapper;
import com.example.service.TopicService;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {

    @Resource
    TopicTypeMapper mapper;

    @Resource
    FlowUtils flowUtils;

    private Set<Integer> types = null;
    @PostConstruct
    private void initTypes() {
        types = this.listTypes()
                .stream()
                .map(TopicType::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<TopicType> listTypes() {
        return mapper.selectList(null);
    }

    @Override
    public String createTopic(int uid, TopicCreateVO vo) {
        if(!textLimitCheck(vo.getContent()))
            return "文章内容太多，发文失败！";
        if(!types.contains(vo.getType()))
            return "文章类型非法！";
        String key = Const.FORUM_TOPIC_CREATE_COUNTER + uid;
        if(!flowUtils.limitPeriodCounterCheck(key, 3, 3600))
            return "发文频繁，请稍后再试！";
        Topic topic = new Topic();
        BeanUtils.copyProperties(vo, topic);
        topic.setContent(vo.getContent().toJSONString());
        topic.setUid(uid);
        topic.setTime(new Date());
        if(this.save(topic)) {
            return null;
        } else {
            return "内部错误，请联系管理员！";
        }
    }

    private boolean textLimitCheck(JSONObject object) {
        if(object == null) return false;
        long length = 0;
        for (Object op : object.getJSONArray("ops")) {
            length += JSONObject.from(op).getString("insert").length();
            if(length > 20000) return false;
        }
        return true;
    }
}
