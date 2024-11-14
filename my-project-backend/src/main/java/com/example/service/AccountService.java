package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.Account;
import com.example.entity.vo.request.*;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.time.Duration;

public interface AccountService extends IService<Account>, UserDetailsService {
    Account findAccountByNameOrEmail(String text);

    String registerEmailVerifyCode(String type, String email, String address);

    String registerEmailAccount(EmailRegisterVO info);

    String resetEmailAccountPassword(EmailResetVO info);

    String resetConfirm(ConfirmResetVO info);

    Account findAccountById(int id);

    String modifyEmail(int id, ModifyEmailVO vo);

    String changePassword(int id, ChangePasswordVO vo);

    String signIn(int id);

    Long getTotalSignInThisMonth(int id);

    Long getContinueSignInThisMonth(int id);

    Boolean online(int id);

    Long getOnlineUserCount();

    Long clearOnlineUser(Duration duration);

    Duration getLastOnlineTime(int id);
}
