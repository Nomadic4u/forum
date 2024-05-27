package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.Interact;
import com.example.entity.dto.Topic;
import com.example.entity.dto.TopicType;
import com.example.entity.vo.request.AddCommentVO;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.request.TopicUpdateVO;
import com.example.entity.vo.response.CommentVO;
import com.example.entity.vo.response.TopicPreviewVO;
import com.example.entity.vo.response.TopicTopVO;
import com.example.entity.vo.response.TopicDetailVO;

import java.util.List;

public interface TopicService extends IService<Topic> {
    List<TopicType> listType();
    String createTopic(int uid, TopicCreateVO vo);
    void updateTopic(int uid, TopicUpdateVO vo);
    List<TopicPreviewVO> listTopicByPage(int pageNumber, int type);
    List<TopicTopVO> topTopics();
    TopicDetailVO getTopic(int tid, int uid);
    void interact(Interact interact, boolean state);
    List<TopicPreviewVO> listCollectTopic(int uid);
    String createComment(AddCommentVO vo, int uid);
    void deleteComment(int id, int uid);
    List<CommentVO> comments(int tid, int pageNumber);
}
