package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.*;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.response.TopicDetailVO;
import com.example.entity.vo.response.TopicPreviewVO;
import com.example.entity.vo.response.TopicTopVO;
import com.example.mapper.*;
import com.example.service.TopicService;
import com.example.utils.CacheUtils;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {

    @Resource
    TopicTypeMapper mapper;

    @Resource
    FlowUtils flowUtils;

    @Resource
    CacheUtils cacheUtils;

    @Resource
    AccountMapper accountMapper;

    @Resource
    AccountDetailsMapper accountDetailsMapper;

    @Resource
    AccountPrivacyMapper accountPrivacyMapper;

    @Resource
    StringRedisTemplate template;

    private Set<Integer> types = null;
    @PostConstruct
    private void initTypes() {
        types = this.listTypes()
                .stream()
                .map(TopicType::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public List<TopicType> listTypes() {
        return mapper.selectList(null);
    }

    @Override
    public String createTopic(int uid, TopicCreateVO vo) {
        if(!textLimitCheck(vo.getContent()))
            return "文章内容太多，发文失败！";
        if(!types.contains(vo.getType()))
            return "文章类型非法！";
        String key = Const.FORUM_TOPIC_CREATE_COUNTER + uid;
        if(!flowUtils.limitPeriodCounterCheck(key, 3, 3600))
            return "发文频繁，请稍后再试！";
        Topic topic = new Topic();
        BeanUtils.copyProperties(vo, topic);
        topic.setContent(vo.getContent().toJSONString());
        topic.setUid(uid);
        topic.setTime(new Date());
        if(this.save(topic)) {
            // 如果添加了新的帖子, 就将之前的缓存全部清除
            cacheUtils.deleteCachePattern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
            return null;
        } else {
            return "内部错误，请联系管理员！";
        }
    }

    @Override
    public List<TopicPreviewVO> listTopicCollects(int uid) {
        return baseMapper.collectTopics(uid)
                .stream()
                .map(topic -> {
                    TopicPreviewVO vo = new TopicPreviewVO();
                    BeanUtils.copyProperties(topic, vo);
                    return vo;
                })
                .toList();
    }

    @Override
    public List<TopicPreviewVO> listTopicByPage(int pageNumber, int type) {
        String key = Const.FORUM_TOPIC_PREVIEW_CACHE + pageNumber + ":" + type;
        List<TopicPreviewVO> list = cacheUtils.takeListFromCache(key, TopicPreviewVO.class);
        if(list != null)
            return list;
        Page<Topic> page = Page.of(pageNumber, 10); // 设置分页参数
        if(type == 0)
            baseMapper.selectPage(page, Wrappers.<Topic>query().orderByDesc("time"));
        else
            baseMapper.selectPage(page, Wrappers.<Topic>query().eq("type", type).orderByDesc("time"));
        List<Topic> topics = page.getRecords(); // 获取当前页的记录列表
        if(topics.isEmpty()) return null;
        list = topics.stream().map(this::resolveToPreview).toList();
        cacheUtils.saveListToCache(key, list, 60);
        return list;
    }

    @Override
    public List<TopicTopVO> listTopTopics() {
        List<Topic> topics = baseMapper.selectList(Wrappers.<Topic>query()
                .select("id", "title", "time")
                .eq("top", 1));
        return topics.stream().map(topic -> {
            TopicTopVO vo = new TopicTopVO();
            BeanUtils.copyProperties(topic, vo);
            return vo;
        }).toList();
    }

    /**
     * 获取帖子详细内容
     * @param tid 帖子ID
     * @return 帖子实体类
     */
    @Override
    public TopicDetailVO getTopic(int tid) {
        TopicDetailVO vo = new TopicDetailVO();
        Topic topic = baseMapper.selectById(tid);
        BeanUtils.copyProperties(topic, vo);
        TopicDetailVO.Interact interact = new TopicDetailVO.Interact(
                hasInteract(tid, topic.getUid(), "like"),
                hasInteract(tid, topic.getUid(), "collect")
        );
        vo.setInteract(interact);
        TopicDetailVO.User user = new TopicDetailVO.User();
        vo.setUser(this.fillUserDetailsByPrivacy(user, topic.getUid()));
        return vo;
    }

    /**
     * 用于用户交互(点赞/收藏)
     * @param interact 交互实体类
     * @param state 交换状态
     */
    @Override
    public void interact(Interact interact, boolean state) {
        String type = interact.getType();
        synchronized (type.intern()) { // 锁的是点赞这个动作
            template.opsForHash().put(type, interact.toKey(), Boolean.toString(state)); // 这个用到了hash的数据结构
            this.saveInteractSchedule(type);
        }
    }

    private boolean hasInteract(int tid, int uid, String type) {
        String key = tid + ":" + uid;
        if (template.opsForHash().hasKey(type, key)) //这里用了一个hash去存贮
            return Boolean.parseBoolean(template.opsForHash().entries(type).get(key).toString());
        return baseMapper.userInteractCount(tid, uid, type) > 0;
    }

    /**
     * 这里实现了一个简单的交互操作调度器, 用于延迟处理交互操作
     */
    // 状态映射, 其中的值表示该类型的交互操作是否在调度中.
    private final Map<String, Boolean> state = new HashMap<>();
    // 定时任务调度器, 创建了一个含有两个线程的线程池
    ScheduledExecutorService service = Executors.newScheduledThreadPool(2);
    private void saveInteractSchedule(String type) {
        if(!state.getOrDefault(type, false)) { // 若不存在或者为false, 说明当前该类型的交互没有被调度
            state.put(type, true); // 此时被调度
            service.schedule(() -> { // 调度任务
                this.saveInteract(type);
                state.put(type, false);
            }, 3, TimeUnit.SECONDS); // 延迟3s后执行, 可以防止频繁操作(例如用户频繁的去点赞, 就可以将3s内的操作合并成一次处理), 这里是后台异步运行
        }
    }

    // 交互数据的持久化
    private void saveInteract(String type) {
        synchronized (type.intern()) {
            // 存储需要添加和删除的交互记录
            List<Interact> check = new LinkedList<>();
            List<Interact> uncheck = new LinkedList<>();
            // 从redis中读取所有的交互记录
            template.opsForHash().entries(type).forEach((k, v) -> { // 先回返回哈希表type中的所有条目, 返回一个map
                if(Boolean.parseBoolean(v.toString()))
                    check.add(Interact.parseInteract(k.toString(), type));// 根据k去创建对象
                else
                    uncheck.add(Interact.parseInteract(k.toString(), type));
            });
            // 保存到数据库
            if(!check.isEmpty())
                baseMapper.addInteract(check, type);
            if(!uncheck.isEmpty())
                baseMapper.deleteInteract(uncheck, type);
            template.delete(type);
        }
    }

    private <T> T fillUserDetailsByPrivacy(T target, int uid){
        AccountDetails details = accountDetailsMapper.selectById(uid);
        Account account = accountMapper.selectById(uid);
        AccountPrivacy accountPrivacy = accountPrivacyMapper.selectById(uid);
        String[] ignores = accountPrivacy.hiddenFields(); // 获取被忽略的字段
        BeanUtils.copyProperties(account, target, ignores);
        BeanUtils.copyProperties(details, target, ignores);
        return target;
    }

    /**
     * 主要用于将Topic转为TopicPreviewVO
     * @param topic Topic对象
     * @return TopicPreviewVO对象
     */
    private TopicPreviewVO resolveToPreview(Topic topic) {
        TopicPreviewVO vo = new TopicPreviewVO();
        BeanUtils.copyProperties(accountMapper.selectById(topic.getUid()), vo);
        BeanUtils.copyProperties(topic, vo);
        vo.setLike(baseMapper.interactCount(topic.getId(), "like"));
        vo.setCollect(baseMapper.interactCount(topic.getId(), "collect"));
        List<String> images = new ArrayList<>();
        StringBuilder previewText = new StringBuilder();
        JSONArray ops = JSONObject.parseObject(topic.getContent()).getJSONArray("ops");
        for (Object op : ops) {
            Object insert = JSONObject.from(op).get("insert");
            if(insert instanceof String text) {
                if(previewText.length() >= 300) continue;
                previewText.append(text);
            } else if(insert instanceof Map<?, ?> map) {
                Optional.ofNullable(map.get("image"))
                        .ifPresent(obj -> images.add(obj.toString()));
            }
        }
        vo.setText(previewText.length() > 300 ? previewText.substring(0, 300) : previewText.toString());
        vo.setImages(images);
        return vo;
    }

    private boolean textLimitCheck(JSONObject object) {
        if(object == null) return false;
        long length = 0;
        for (Object op : object.getJSONArray("ops")) {
            length += JSONObject.from(op).getString("insert").length();
            if(length > 20000) return false;
        }
        return true;
    }
}
