package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.dto.Topic;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TopicMapper extends BaseMapper<Topic> {
    @Select("""
            select * from db_topic letf join db_account on uid = db_account.id
             order by `time` desc limit ${start}, 10
            """)
    List<Topic> topicList(int start);

    @Select("""
            select * from db_topic letf join db_account on uid = db_account.id
             where type = #{type}
             order by `time` desc limit ${start}, 10
            """)
    List<Topic> topicListByType(int start, int type);
}
