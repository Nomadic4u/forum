package com.example.controller;

import com.example.entity.RestBean;
import com.example.entity.dto.Account;
import com.example.entity.dto.AccountDetails;
import com.example.entity.vo.request.*;
import com.example.entity.vo.response.AccountDetailsVO;
import com.example.entity.vo.response.AccountPrivacyVO;
import com.example.entity.vo.response.AccountVO;
import com.example.service.AccountDetailsService;
import com.example.service.AccountPrivacyService;
import com.example.service.AccountService;
import com.example.utils.Const;
import com.example.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Optional;

@Validated
@RestController
@RequestMapping("/api/user")
public class AccountController {

    @Resource
    AccountService accountservice;

    @Resource
    AccountDetailsService detailsService;

    @Resource
    AccountPrivacyService privacyService;

    @Resource
    ControllerUtils utils;

    /**
     * 查询用户个人信息
     *
     * @param id 用户id, 这里经过了预处理, 放在了request中
     * @return 用户实体类
     */
    @GetMapping("/info")
    public RestBean<AccountVO> info(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        Account account = accountservice.findAccountById(id);
        return RestBean.success(account.asViewObject(AccountVO.class));
    }

    /**
     * 查询用户详细信息
     *
     * @param id 用户id
     * @return 用户详细信息实体类
     */
    @GetMapping("/details")
    public RestBean<AccountDetailsVO> details(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        AccountDetails details = Optional
                .ofNullable(detailsService.findAccountDetailsById(id))
                .orElseGet(AccountDetails::new);
        return RestBean.success(details.asViewObject(AccountDetailsVO.class));
    }

    /**
     * 用户保存个人信息信息
     *
     * @param id 用户id
     * @param vo 用户详细信息vo类
     * @return 用户详细信息实体类
     */
    @PostMapping("/save-details")
    public RestBean<Void> saveDetails(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid DetailsSaveVO vo) {
        boolean success = detailsService.saveAccountDetails(id, vo);
        return success ? RestBean.success() : RestBean.failure(400, "此用户名已被其他用户使用，请重新更换！");
    }

    /**
     * 更改邮箱
     *
     * @param id 用户id
     * @param vo 更改邮箱实体类
     * @return 是否成功
     */
    @PostMapping("/modify-email")
    public RestBean<Void> modifyEmail(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid ModifyEmailVO vo) {
        return utils.messageHandle(() -> accountservice.modifyEmail(id, vo));
    }

    /**
     * 修改密码
     *
     * @param id 用户id
     * @param vo 修改密码实体类
     * @return 是否成功
     */
    @PostMapping("/change-password")
    public RestBean<Void> changePassword(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                         @RequestBody @Valid ChangePasswordVO vo) {
        return utils.messageHandle(() -> accountservice.changePassword(id, vo));
    }

    /**
     * 用户隐私设置, 是否展示对应的隐私
     *
     * @param id 用户id
     * @param vo 隐私展示实体类
     * @return 是否成功
     */
    @PostMapping("/save-privacy")
    public RestBean<Void> savePrivacy(@RequestAttribute(Const.ATTR_USER_ID) int id,
                                      @RequestBody @Valid PrivacySaveVO vo) {
        privacyService.savePrivacy(id, vo);
        return RestBean.success();
    }

    /**
     * 获取可展示的隐私信息
     *
     * @param id 用户id
     * @return 可展示隐私信息实体类
     */
    @GetMapping("/privacy")
    public RestBean<AccountPrivacyVO> privacy(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(privacyService.accountPrivacy(id).asViewObject(AccountPrivacyVO.class));
    }

    /**
     * 用户签到
     *
     * @param id 用户id
     * @return 是否成功
     */
    @PostMapping("/sign-in")
    public RestBean<String> signInToday(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        String message = accountservice.signIn(id);
        if (message != null)
            return RestBean.failure(400, message);
        return RestBean.success();
    }

    /**
     * 用户获取本月签到总数
     *
     * @param id
     * @return
     */
    @GetMapping("/get-month-sign")
    public RestBean<Long> getTotalSignInThisMonth(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(accountservice.getTotalSignInThisMonth(id));
    }

    /**
     * 用户获取本月连续签到总数
     * @param id
     * @return
     */
    @GetMapping("/get-month-sign-continue")
    public RestBean<Long> getContinueSignInThisMonth(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(accountservice.getContinueSignInThisMonth(id));
    }

    /**
     * 每5分钟用户发送心跳
     * @param id 用户id
     * @return
     */
    @PostMapping("/online")
    public RestBean<Void> userOnline(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return accountservice.online(id) ? RestBean.success() : RestBean.failure(400, "心跳发送失败");
    }

    /**
     * 获取在线用户数量
     * @param id
     * @return
     */
    @GetMapping("/get-online-user-count")
    public RestBean<Long> getOnlineUserCount(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(accountservice.getOnlineUserCount());
    }

    /**
     * 清除一段时间没在线的用户
     * @param duration
     * @return
     */
    @DeleteMapping("/clear-online-user")
    public RestBean<Long> clearOnlineUser(@RequestAttribute Duration duration) {
        return RestBean.success(accountservice.clearOnlineUser(duration));
    }

    /**
     * 获取指定用户最后一次在线的时间差
     * @param id
     * @return
     */
    @GetMapping("/get-last-online-time")
    public RestBean<Duration> getLastOnlineTime(@RequestAttribute(Const.ATTR_USER_ID) int id) {
        return RestBean.success(accountservice.getLastOnlineTime(id));
    }



}
