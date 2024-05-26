package com.example.controller;

import com.example.entity.RestBean;
import com.example.entity.dto.Interact;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.response.*;
import com.example.service.TopicService;
import com.example.service.WeatherService;
import com.example.utils.Const;
import com.example.utils.ControllerUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/forum")
public class ForumController {

    @Resource
    WeatherService service;

    @Resource
    TopicService topicService;

    @Resource
    ControllerUtils utils;

    /**
     * 获取对应城市的天气信息
     * @param longitude 经度
     * @param latitude 纬度
     * @return 天气信息实例类
     */
    @GetMapping("/weather")
    public RestBean<WeatherVO> weather(double longitude, double latitude){
        WeatherVO vo = service.fetchWeather(longitude, latitude);
        return vo == null ?
                RestBean.failure(400, "获取地理位置信息与天气失败，请联系管理员！") : RestBean.success(vo);
    }

    /**
     * 获取帖子类型介绍
     * @return 类型实体类
     */
    @GetMapping("/types")
    public RestBean<List<TopicTypeVO>> listTypes(){
        return RestBean.success(topicService
                .listTypes()
                .stream()
                .map(type -> type.asViewObject(TopicTypeVO.class))
                .toList());
    }

    /**
     * 发布新的帖子
     * @param vo 帖子实体类
     * @param id 用户id
     * @return 是否成功
     */
    @PostMapping("/create-topic")
    public RestBean<Void> createTopic(@Valid @RequestBody TopicCreateVO vo,
                                      @RequestAttribute(Const.ATTR_USER_ID) int id) {
        return utils.messageHandle(() -> topicService.createTopic(id, vo));
    }

    /**
     * 帖子展示
     * @param page 页码
     * @param type 帖子类型
     * @return 帖子实体类
     */
    @GetMapping("/list-topic")
    public RestBean<List<TopicPreviewVO>> listTopic(@RequestParam @Min(0) int page,
                                                    @RequestParam @Min(0) int type) {
        return RestBean.success(topicService.listTopicByPage(page + 1, type));
    }

    /**
     * 获取置顶帖子列表
     * @return 帖子实体类
     */
    @GetMapping("/top-topic")
    public RestBean<List<TopicTopVO>> topTopic(){
        return RestBean.success(topicService.listTopTopics());
    }

    /**
     * 帖子详细内容
     * @param tid 帖子id
     * @return 帖子实体类
     */
    @GetMapping("/topic")
    public RestBean<TopicDetailVO> topic(@RequestParam @Min(0) int tid){
        return RestBean.success(topicService.getTopic(tid));
    }

    /**
     * 处理用户的交互(点赞/收藏)请求
     * @param tid 帖子ID
     * @param type 交互类型
     * @param state 是添加/取消
     * @param id 用户ID
     * @return 是否成功
     */
    @GetMapping("/interact")
    public RestBean<Void> interact(@RequestParam @Min(0) int tid,
                                   @RequestParam @Pattern(regexp = "(like|collect)") String type,
                                   @RequestParam boolean state,
                                   @RequestAttribute(Const.ATTR_USER_ID) int id) {
        topicService.interact(new Interact(tid, id, new Date(), type), state);
        return RestBean.success();
    }

    @GetMapping("/collects")
    public RestBean<List<TopicPreviewVO>> collects(@RequestAttribute(Const.ATTR_USER_ID) int id){
        return RestBean.success(topicService.listTopicCollects(id));
    }
}
