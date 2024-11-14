package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.dto.TopicLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TopicLikeMapper extends BaseMapper<TopicLike> {
    @Select("SELECT tid FROM db_topic_interact_like WHERE uid = #{uid}")
    List<Integer> selectTidsByUid(Integer uid);
}
