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

    public static Interact parseInteract(String key, String type) {
        String[] keys = key.split(":");
        return new Interact(Integer.parseInt(keys[0]),
                            Integer.parseInt(keys[1]),
                            new Date(), type);
    }

    public String toKey() {
        return this.tid + ":" + this.uid;
    }
}
