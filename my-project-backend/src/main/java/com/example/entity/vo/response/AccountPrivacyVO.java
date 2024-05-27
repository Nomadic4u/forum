package com.example.entity.vo.response;

import lombok.Data;

@Data
public class AccountPrivacyVO {
    boolean phone = true;
    boolean email = true;
    boolean wx = true;
    boolean qq = true;
    boolean gender = true;
}
