package com.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.RestBean;
import com.example.entity.dto.Account;
import com.example.entity.dto.AccountDetails;
import com.example.entity.dto.AccountPrivacy;
import com.example.entity.vo.request.*;
import com.example.mapper.AccountDetailsMapper;
import com.example.mapper.AccountMapper;
import com.example.mapper.AccountPrivacyMapper;
import com.example.service.AccountService;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import jakarta.annotation.Resource;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Local;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 账户信息处理相关服务
 */
@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    //验证邮件发送冷却时间限制，秒为单位
    @Value("${spring.web.verify.mail-limit}")
    int verifyLimit;

    @Resource
    AmqpTemplate rabbitTemplate;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    RedisTemplate redisTemplate;

    @Resource
    PasswordEncoder passwordEncoder;

    @Resource
    FlowUtils flow;

    @Resource
    AccountDetailsMapper detailsMapper;

    @Resource
    AccountPrivacyMapper privacyMapper;

    /**
     * 从数据库中通过用户名或邮箱查找用户详细信息
     *
     * @param username 用户名
     * @return 用户详细信息
     * @throws UsernameNotFoundException 如果用户未找到则抛出此异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = this.findAccountByNameOrEmail(username);
        if (account == null) throw new UsernameNotFoundException("用户名或密码错误");
        return User.withUsername(username).password(account.getPassword()).roles(account.getRole()).build();
    }

    /**
     * 生成注册验证码存入Redis中，并将邮件发送请求提交到消息队列等待发送
     *
     * @param type    类型
     * @param email   邮件地址
     * @param address 请求IP地址
     * @return 操作结果，null表示正常，否则为错误原因
     */
    public String registerEmailVerifyCode(String type, String email, String address) {
        synchronized (address.intern()) {
            if (!this.verifyLimit(address)) return "请求频繁，请稍后再试";
            Random random = new Random();
            int code = random.nextInt(899999) + 100000;
            Map<String, Object> data = Map.of("type", type, "email", email, "code", code);
            rabbitTemplate.convertAndSend(Const.MQ_MAIL, data);
            stringRedisTemplate.opsForValue().set(Const.VERIFY_EMAIL_DATA + email, String.valueOf(code), 3, TimeUnit.MINUTES);
            return null;
        }
    }

    /**
     * 邮件验证码注册账号操作，需要检查验证码是否正确以及邮箱、用户名是否存在重名
     *
     * @param info 注册基本信息
     * @return 操作结果，null表示正常，否则为错误原因
     */
    public String registerEmailAccount(EmailRegisterVO info) {
        String email = info.getEmail();
        String code = this.getEmailVerifyCode(email);
        if (code == null) return "请先获取验证码";
        if (!code.equals(info.getCode())) return "验证码错误，请重新输入";
        if (this.existsAccountByEmail(email)) return "该邮件地址已被注册";
        String username = info.getUsername();
        if (this.existsAccountByUsername(username)) return "该用户名已被他人使用，请重新更换";
        String password = passwordEncoder.encode(info.getPassword());
        Account account = new Account(null, info.getUsername(), password, email, Const.ROLE_DEFAULT, null, new Date());
        if (!this.save(account)) {
            return "内部错误，注册失败";
        } else {
            this.deleteEmailVerifyCode(email);
            privacyMapper.insert(new AccountPrivacy(account.getId()));
            AccountDetails details = new AccountDetails();
            details.setId(account.getId());
            detailsMapper.insert(details);
            return null;
        }
    }

    /**
     * 邮件验证码重置密码操作，需要检查验证码是否正确
     *
     * @param info 重置基本信息
     * @return 操作结果，null表示正常，否则为错误原因
     */
    @Override
    public String resetEmailAccountPassword(EmailResetVO info) {
        String verify = resetConfirm(new ConfirmResetVO(info.getEmail(), info.getCode()));
        if (verify != null) return verify;
        String email = info.getEmail();
        String password = passwordEncoder.encode(info.getPassword());
        boolean update = this.update().eq("email", email).set("password", password).update();
        if (update) {
            this.deleteEmailVerifyCode(email);
        }
        return update ? null : "更新失败，请联系管理员";
    }

    /**
     * 重置密码确认操作，验证验证码是否正确
     *
     * @param info 验证基本信息
     * @return 操作结果，null表示正常，否则为错误原因
     */
    @Override
    public String resetConfirm(ConfirmResetVO info) {
        String email = info.getEmail();
        String code = this.getEmailVerifyCode(email);
        if (code == null) return "请先获取验证码";
        if (!code.equals(info.getCode())) return "验证码错误，请重新输入";
        return null;
    }

    /**
     * 移除Redis中存储的邮件验证码
     *
     * @param email 电邮
     */
    private void deleteEmailVerifyCode(String email) {
        String key = Const.VERIFY_EMAIL_DATA + email;
        stringRedisTemplate.delete(key);
    }

    /**
     * 获取Redis中存储的邮件验证码
     *
     * @param email 电邮
     * @return 验证码
     */
    private String getEmailVerifyCode(String email) {
        String key = Const.VERIFY_EMAIL_DATA + email;
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 针对IP地址进行邮件验证码获取限流
     *
     * @param address 地址
     * @return 是否通过验证
     */
    private boolean verifyLimit(String address) {
        String key = Const.VERIFY_EMAIL_LIMIT + address;
        return flow.limitOnceCheck(key, verifyLimit);
    }

    /**
     * 通过用户名或邮件地址查找用户
     *
     * @param text 用户名或邮件
     * @return 账户实体
     */
    public Account findAccountByNameOrEmail(String text) {
        return this.query().eq("username", text).or().eq("email", text).one();
    }

    @Override
    public Account findAccountById(int id) {
        return this.query().eq("id", id).one();
    }

    @Override
    public String modifyEmail(int id, ModifyEmailVO vo) {
        String code = getEmailVerifyCode(vo.getEmail());
        if (code == null) return "请先获取验证码";
        if (!code.equals(vo.getCode())) return "验证码错误，请重新输入";
        this.deleteEmailVerifyCode(vo.getEmail());
        Account account = this.findAccountByNameOrEmail(vo.getEmail());
        if (account != null && account.getId() != id) return "该邮件已被其他账号绑定，无法完成操作";
        this.update().set("email", vo.getEmail()).eq("id", id).update();
        return null;
    }

    @Override
    public String changePassword(int id, ChangePasswordVO vo) {
        String passwd = this.query().eq("id", id).one().getPassword();
        if (!passwordEncoder.matches(vo.getPassword(), passwd)) return "原密码错误，请重新输入！";
        boolean success = this.update().eq("id", id).set("password", passwordEncoder.encode(vo.getNew_password())).update();
        return success ? null : "未知错误，请联系管理员";
    }

    /**
     * 查询指定邮箱的用户是否已经存在
     *
     * @param email 邮箱
     * @return 是否存在
     */
    private boolean existsAccountByEmail(String email) {
        return this.baseMapper.exists(Wrappers.<Account>query().eq("email", email));
    }

    /**
     * 查询指定用户名的用户是否已经存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    private boolean existsAccountByUsername(String username) {
        return this.baseMapper.exists(Wrappers.<Account>query().eq("username", username));
    }


    /**
     * 用户当日签到
     *
     * @param id 用户id
     * @return 当前次数
     */
    @Override
    public String signIn(int id) {
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = Const.USER_SIGN_KEY + id + keySuffix;
        int dayOfMonth = now.getDayOfMonth();
        Boolean bit = stringRedisTemplate.opsForValue().getBit(key, dayOfMonth);
        if (bit.equals(true)) {
            return "今日已签到, 不可重复签到~";
        }
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        // 计算连续签到
        long count = 0;
        List<Long> result = stringRedisTemplate.opsForValue().bitField(key, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (result == null || result.isEmpty()) {
            count = 0;
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            count = 0;
        } else {
            while (true) {
                if ((num & 1) == 0) {
                    break;
                } else {
                    count++;
                }
                num >>>= 1;
            }
        }
        String continue_key = Const.Continue_User + id + keySuffix;
        stringRedisTemplate.opsForValue().set(continue_key, String.valueOf(count), 24, TimeUnit.HOURS);
        return null;
    }

    /**
     * 用户本月签到的天数总数
     *
     * @param id
     * @return
     */
    @Override
    public Long getTotalSignInThisMonth(int id) {
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = Const.USER_SIGN_KEY + id + keySuffix;
        long count = (long) redisTemplate.execute((RedisCallback) con -> con.bitCount(key.getBytes()));
        return count;
    }

    /**
     * 用户本月连续签到的天数
     *
     * @param id
     * @return
     */
    @Override
    public Long getContinueSignInThisMonth(int id) {
        LocalDateTime now = LocalDateTime.now();
        String keySuffix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = Const.USER_SIGN_KEY + id + keySuffix;
        int dayOfMonth = now.getDayOfMonth();

        // 查询缓存
        String continue_key = Const.Continue_User + id + keySuffix;
        String res = stringRedisTemplate.opsForValue().get(continue_key);
        if(res != null){
            return Long.valueOf(res);
        }

        List<Long> result = stringRedisTemplate.opsForValue().bitField(key, BitFieldSubCommands.create().get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0));
        if (result == null || result.isEmpty()) {
            return 0L;
        }
        Long num = result.get(0);
        if (num == null || num == 0) {
            return 0L;
        }
        long count = 0;
        while (true) {
            if ((num & 1) == 0) {
                break;
            } else {
                count++;
            }
            num >>>= 1;
        }
        stringRedisTemplate.opsForValue().set(continue_key, String.valueOf(count), 25, TimeUnit.HOURS);
        return count;
    }

    /**
     * 添加/更新用户在线信息
     *
     * @param id 用户id
     * @return 是否成功
     */
    @Override
    public Boolean online(int id) {
        return stringRedisTemplate.opsForZSet().add(Const.Online_User, String.valueOf(id), Instant.now().toEpochMilli());
    }

    /**
     * 获取当前在线的用户数量
     *
     * @return 在线用户数
     */
    @Override
    public Long getOnlineUserCount() {
        return stringRedisTemplate.opsForZSet().zCard(Const.Online_User);
    }

    /**
     * 清除的用户数量
     *
     * @param duration 时间范围
     * @return 清除用户数量
     */
    @Override
    public Long clearOnlineUser(Duration duration) {

        return stringRedisTemplate.opsForZSet().removeRangeByScore(Const.Online_User, 0, Instant.now().minus(duration).toEpochMilli());
    }

    /**
     * 获取指定用户最后一次在线的时间差
     *
     * @param id 用户id
     * @return 时间差
     */
    @Override
    public Duration getLastOnlineTime(int id) {
        Double result = stringRedisTemplate.opsForZSet().score(Const.Online_User, String.valueOf(id));

        return result == null ? null : Duration.ofMillis(result.longValue());
    }


}
