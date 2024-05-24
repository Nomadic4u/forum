package com.example.entity.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.entity.BaseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data  // Lombok 注解，生成 getter、setter 等方法
@TableName("db_account_details")  // MyBatis 注解，指定表名为 "db_account_details"
@AllArgsConstructor  // Lombok 注解，生成全参构造函数
@NoArgsConstructor  // Lombok 注解，生成无参构造函数
public class AccountDetails implements BaseData {  // 类定义，实现 BaseData 接口
    @TableId  // MyBatis 注解，指定主键
    Integer id;  // 主键字段

    int gender;  // 性别字段
    String phone;  // 电话号码字段
    String qq;  // QQ 号字段
    String wx;  // 微信号字段
    @TableField("`desc`")  // MyBatis 注解，指定列名为 "desc"
    String desc;  // 描述字段
}

