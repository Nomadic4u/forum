package com.example.entity.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("db_topic_interact_like")
public class TopicLike {
    Integer tid;
    Integer uid;
    Date time;
}
