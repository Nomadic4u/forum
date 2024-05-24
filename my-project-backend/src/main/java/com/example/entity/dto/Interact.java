package com.example.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class Interact {
    Integer tid;
    Integer uid;
    Date time;
    String type;

    // 生成键的方法，格式为 "tid:uid"
    public String toKey() {
        return tid + ":" + uid;
    }

    // 将字符串解析为 Interact 对象的静态方法
    public static Interact parseInteract(String str, String type) {
        String[] keys = str.split(":");
        return new Interact(Integer.parseInt(keys[0]), Integer.parseInt(keys[1]), new Date(), type);
    }
}