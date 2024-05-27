<script setup>
import {useRoute} from "vue-router";
import {get, post} from "@/net";
import {computed, reactive, ref} from "vue";
import axios from "axios";
import {ArrowLeft, ChatSquare, CircleCheck, Delete, EditPen, Female, Male, Plus, Star} from "@element-plus/icons-vue";
import { QuillDeltaToHtmlConverter } from 'quill-delta-to-html';
import {useStore} from "@/store";
import Card from "@/components/Card.vue";
import router from "@/router";
import InteractButton from "@/components/InteractButton.vue";
import {ElMessage} from "element-plus";
import TopicTag from "@/components/TopicTag.vue";
import TopicEditor from "@/components/TopicEditor.vue";
import TopicCommentEditor from "@/components/TopicCommentEditor.vue";

const route = useRoute()
const store = useStore()

const tid = route.params.tid

const topic = reactive({
    data: null,
    like: false,
    collect: false,
    comments: [],
    page: 1
})
const edit = ref(false)
const comment = reactive({
    show: false,
    text: '',
    quote: null
})

function init() {
    get(`/api/forum/topic?tid=${tid}`, data => {
        topic.data = data
        topic.like = data.interact.like
        topic.collect = data.interact.collect
    })
    loadComments(0)
}
init()

function convertToHtml(content){
    const ops = JSON.parse(content).ops
    const converter = new QuillDeltaToHtmlConverter(ops, {inlineStyles: true});
    return converter.convert();
}

function interact(type, message) {
    get(`/api/forum/interact?tid=${tid}&type=${type}&state=${!topic[type]}`, () => {
        topic[type] = !topic[type]
        if(topic[type])
            ElMessage.success(`${message}成功!`)
        else
            ElMessage.success(`已取消${message}!`)
    })
}

function updateTopic(editor) {
    post('/api/forum/update-topic', {
        id: tid,
        type: editor.type,
        title: editor.title,
        content: editor.text
    }, () => {
        ElMessage.success("帖子内容更新成功！")
        edit.value = false
        init()
    })
}

function loadComments(page){
    topic.comments = null
    topic.page = page + 1
    get(`/api/forum/comments?tid=${tid}&page=${page}`, data => topic.comments = data)
}

function deleteComment(id) {
    get(`/api/forum/delete-comment?id=${id}`, () => {
        ElMessage.success('删除评论成功！')
        loadComments(topic.page - 1)
    })
}
</script>

<template>
    <div class="topic-page" v-if="topic.data">
        <div class="topic-main" style="position: sticky;top: 0;z-index: 10">
            <card style="display: flex;width: 100%">
                <el-button type="info" :icon="ArrowLeft" size="small"
                           round plain @click="router.push('/index')">返回列表</el-button>
                <div style="text-align: center;flex: 1;height: 25px">
                    <topic-tag :type="topic.data.type"/>
                    <span style="margin-left: 5px;font-weight: bold">{{topic.data.title}}</span>
                </div>
            </card>
        </div>
        <div class="topic-main">
            <div class="topic-main-left">
                <el-avatar :src="store.userAvatarUrl(topic.data.user.avatar)"
                           :size="60"></el-avatar>
                <div style="margin-left: 10px">
                    <div style="font-weight: bold;font-size: 18px">
                        {{topic.data.user.username}}
                        <span style="color: hotpink" v-if="topic.data.user.gender">
                            <el-icon><Female/></el-icon>
                        </span>
                        <span style="color: dodgerblue" v-if="topic.data.user.gender === false">
                            <el-icon><Male/></el-icon>
                        </span>
                    </div>
                    <div class="desc">{{topic.data.user.email || '已隐藏电子邮件'}}</div>
                </div>
                <el-divider style="margin: 10px 0"></el-divider>
                <div style="text-align: left;margin: 0 5px">
                    <div class="desc">微信号: {{topic.data.user.wx}}</div>
                    <div class="desc">QQ号: {{topic.data.user.qq}}</div>
                    <div class="desc">手机号: {{topic.data.user.phone}}</div>
                </div>
                <el-divider style="margin: 10px 0"></el-divider>
                <div class="desc" style="margin: 0 5px">{{topic.data.user.desc}}</div>
            </div>
            <div class="topic-main-right">
                <div class="topic-content" v-html="convertToHtml(topic.data.content)"></div>
                <el-divider/>
                <div style="font-size: 13px;color: grey;text-align: center">
                    <div>发帖时间: {{new Date(topic.data.time).toLocaleString()}}</div>
                </div>
                <div style="text-align: right;margin-top: 30px">
                    <interact-button name="编辑帖子" color="dodgerblue" style="margin-right: 20px"
                                     :check="false" @check="edit = true" v-if="store.user.id === topic.data.user.uid">
                        <el-icon><EditPen /></el-icon>
                    </interact-button>
                    <interact-button name="点个赞吧" check-name="已点赞" color="pink"
                                     :check="topic.like" @check="interact('like', '点赞')">
                        <el-icon><CircleCheck /></el-icon>
                    </interact-button>
                    <interact-button name="收藏帖子" check-name="已收藏" color="orange"
                                     :check="topic.collect" @check="interact('collect', '收藏')"
                                     style="margin-left: 20px">
                        <el-icon><Star/></el-icon>
                    </interact-button>
                </div>
            </div>
        </div>
        <transition name="el-fade-in-linear" mode="out-in">
            <div v-if="topic.comments">
                <div class="topic-main" style="margin-top: 10px" v-for="item in topic.comments">
                    <div class="topic-main-left">
                        <el-avatar :src="store.userAvatarUrl(item.user.avatar)"
                                   :size="60"></el-avatar>
                        <div style="margin-left: 10px">
                            <div style="font-weight: bold;font-size: 18px">
                                {{item.user.username}}
                                <span style="color: hotpink" v-if="item.user.gender">
                            <el-icon><Female/></el-icon>
                        </span>
                                <span style="color: dodgerblue" v-if="item.user.gender === false">
                            <el-icon><Male/></el-icon>
                        </span>
                            </div>
                            <div class="desc">{{item.user.email || '已隐藏电子邮件'}}</div>
                        </div>
                        <el-divider style="margin: 10px 0"></el-divider>
                        <div style="text-align: left;margin: 0 5px">
                            <div class="desc">微信号: {{item.user.wx}}</div>
                            <div class="desc">QQ号: {{item.user.qq}}</div>
                            <div class="desc">手机号: {{item.user.phone}}</div>
                        </div>
                    </div>
                    <div class="topic-main-right" style="display: flex;flex-direction: column">
                        <div style="font-size: 13px;color: grey">
                            <div>评论时间: {{new Date(item.time).toLocaleString()}}</div>
                        </div>
                        <div v-if="item.quote" class="comment-quote">
                            回复: {{item.quote}}
                        </div>
                        <div class="topic-content" style="flex: 1" v-html="convertToHtml(item.content)"></div>
                        <div style="text-align: right">
                            <el-link :icon="ChatSquare" @click="comment.show = true;comment.quote = item"
                                     type="info">&nbsp;回复评论</el-link>
                            <el-link :icon="Delete" type="danger" v-if="item.user.id === store.user.id"
                                     @click="deleteComment(item.id)"
                                     style="margin-left: 20px">&nbsp;删除评论</el-link>
                        </div>
                    </div>
                </div>
                <div style="width: fit-content;margin: 20px auto">
                    <el-pagination background layout="prev, pager, next"
                                   v-model:current-page="topic.page"
                                   @current-change="number => loadComments(number - 1)"
                                   :total="topic.data.commentCount" :page-size="10" hide-on-single-page/>
                </div>
            </div>
        </transition>
        <topic-editor :show="edit" @close="edit = false" v-if="topic.data"
                      :default-title="topic.data.title" :default-text="topic.data.content"
                      :default-type="topic.data.type" submit-button="更新帖子内容"
                      :submit="updateTopic"/>
        <topic-comment-editor :show="comment.show" @close="comment.show = false"
                              :quote="comment.quote" :tid="tid"
                              @comment="comment.show = false;loadComments(Math.max(0, Math.ceil(topic.data.commentCount / 10) - 1))"/>
        <div class="add-comment" @click="comment.show = true">
            <el-icon><Plus /></el-icon>
        </div>
    </div>
</template>

<style scoped>
.comment-quote {
    font-size: 13px;
    color: grey;
    background-color: rgba(94, 94, 94, 0.2);
    padding: 10px;
    margin-top: 10px;
    border-radius: 5px;
}

.add-comment {
    position: fixed;
    bottom: 20px;
    right: 20px;
    width: 40px;
    height: 40px;
    line-height: 45px;
    font-size: 18px;
    color: var(--el-color-primary);
    text-align: center;
    border-radius: 20px;
    background: var(--el-bg-color-overlay);
    box-shadow: var(--el-box-shadow-lighter);

    &:hover {
        background: var(--el-border-color-extra-light);
        cursor: pointer;
    }
}

.topic-page {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 10px 0;
}

.topic-main {
    display: flex;
    border-radius: 7px;
    margin: 0 auto;
    background-color: var(--el-bg-color);
    width: 800px;

    .topic-main-left {
        width: 200px;
        padding: 10px;
        text-align: center;
        border-right: solid 1px var(--el-border-color);

        .desc {
            font-size: 13px;
            color: grey;
        }
    }

    .topic-main-right {
        width: 600px;
        padding: 10px 20px;

        .topic-content {
            font-size: 14px;
            line-height: 22px;
            opacity: 0.8;
        }
    }
}
</style>
