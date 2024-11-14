package com.example.entity.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("db_sensitive_word")
public class SensitiveWord {
    private int id;
    private String word;
}
