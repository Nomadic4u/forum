package com.example.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.AccountPrivacy;
import com.example.entity.vo.request.PrivacySaveVO;
import com.example.mapper.AccountPrivacyMapper;
import com.example.service.AccountPrivacyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AccountPrivacyServiceImpl extends ServiceImpl<AccountPrivacyMapper, AccountPrivacy> implements AccountPrivacyService {

    @Override
    @Transactional
    public void savePrivacy(int id, PrivacySaveVO vo) {
        AccountPrivacy privacy = Optional.ofNullable(this.getById(id)).orElse(new AccountPrivacy(id));
        switch (vo.getType()) {
            case "phone" -> privacy.setPhone(vo.isStatus());
            case "email" -> privacy.setEmail(vo.isStatus());
            case "gender" -> privacy.setGender(vo.isStatus());
            case "wx" -> privacy.setWx(vo.isStatus());
            case "qq" -> privacy.setQq(vo.isStatus());
        }
        this.saveOrUpdate(privacy);
    }

    @Override
    public AccountPrivacy accountPrivacy(int id) {
        return this.getById(id);
    }
}
