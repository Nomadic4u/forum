package com.example.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("db_topic")
public class Topic {
    @TableId(type = IdType.AUTO)
    Integer id;
    String title; // 标题
    String content; // 内容
    Integer type; // 类型
    Date time;
    Integer uid;
}
