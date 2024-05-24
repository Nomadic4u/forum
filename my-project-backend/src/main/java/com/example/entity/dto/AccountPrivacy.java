package com.example.entity.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.entity.BaseData;
import lombok.Data;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

@Data  // Lombok 注解，生成 getter、setter 等方法
@TableName("db_account_privacy")  // MyBatis 注解，指定表名为 "db_account_privacy"
public class AccountPrivacy implements BaseData {  // 类定义，实现 BaseData 接口
    @TableId(type = IdType.AUTO)  // MyBatis 注解，指定自增主键
    final Integer id;  // 主键字段

    boolean phone = true;  // 电话字段，默认值为 true
    boolean email = true;  // 邮箱字段，默认值为 true
    boolean wx = true;  // 微信字段，默认值为 true
    boolean qq = true;  // QQ 字段，默认值为 true
    boolean gender = true;  // 性别字段，默认值为 true

    // 获取所有隐藏字段的方法
    public String[] hiddenFields() {
        List<String> strings = new LinkedList<>();  // 用于存储隐藏字段名称的列表
        Field[] fields = this.getClass().getDeclaredFields();  // 获取所有声明的字段
        for (Field field : fields) {  // 遍历字段
            try {
                // 检查字段类型是否为 boolean 且值是否为 false
                if (field.getType().equals(boolean.class) && !field.getBoolean(this))
                    strings.add(field.getName());  // 添加字段名称到列表
            } catch (Exception ignored) {}  // 忽略异常
        }
        return strings.toArray(String[]::new);  // 将列表转换为字符串数组并返回
    }
}

