package com.example.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SensitiveWordMapper {

    @Select("SELECT word FROM db_sensitive_word")
    List<String> selectAllSensitiveWords();

}
