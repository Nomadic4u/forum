<script setup>
import LightCard from "@/components/LightCard.vue";
import {
    Calendar,
    Clock,
    CollectionTag,
    Compass,
    Document,
    Picture,
    Edit,
    EditPen,
    Link,
    Microphone, CircleCheck, Star, ArrowRightBold, FolderOpened
} from "@element-plus/icons-vue";
import {computed, reactive, ref, watch} from "vue"
import Weather from "@/components/Weather.vue";
import {get} from "@/net";
import {ElMessage} from "element-plus";
import TopicEditor from "@/components/TopicEditor.vue";
import {useStore} from "@/store";
import axios from "axios";
import router from "@/router";
import TopicCollectList from "@/components/TopicCollectList.vue";
import TopicTag from "@/components/TopicTag.vue";

const store = useStore()

const weather = reactive({
    now: {},
    location: {},
    hourly: [],
    success: false
})
const editor = ref(false)
const topics = reactive({
    page: 0,
    list: [],
    type: 0,
    end: false,
    top: []
})
const collects = ref(false)

watch(() => topics.type, () => {
    topics.page = 0
    topics.end = false
    topics.list = []
    updateList()
}, {immediate: true})

const today = computed(() => {
    const date = new Date()
    return `${date.getFullYear()} 年 ${date.getMonth() + 1} 月 ${date.getDate()} 日`
})

function updateList() {
    if(topics.end) return
    get(`/api/forum/list-topic?page=${topics.page}&type=${topics.type}`, data => {
        if(data) {
            data.forEach(d => topics.list.push(d))
            topics.page++
        }
        if(!data || data.length < 10)
            topics.end = true
    })
}
get('/api/forum/top-topic', data => topics.top = data)

function onTopicCreate() {
    editor.value = false;
    topics.page = 0
    topics.end = false
    topics.list = []
    updateList()
}

navigator.geolocation.getCurrentPosition(position => {
    const latitude = position.coords.latitude
    const longitude = position.coords.longitude
    get(`/api/forum/weather?latitude=${latitude}&longitude=${longitude}`, data => {
        Object.assign(weather, data)
        weather.success = true
    })
}, error => {
    console.info(error)
    ElMessage.warning('获取位置信息超时，请检测浏览器设置')
    get(`/api/forum/weather?latitude=39.90499&longitude=116.40529`, data => {
        Object.assign(weather, data)
        weather.success = true
    })
}, {
    timeout: 3000,
    enableHighAccuracy: true
})
</script>

<template>
    <div style="display: flex;margin: 20px auto;gap: 20px;max-width: 1000px">
        <div style="flex: 1">
            <light-card>
                <div class="edit-topic">
                    <div @click="editor = true">
                        <el-icon>
                            <EditPen/>
                        </el-icon>
                        点击发表主题...
                    </div>
                </div>
                <div style="margin-top: 10px;display: flex;gap: 13px;font-size: 18px;color: grey">
                    <el-icon><Edit /></el-icon>
                    <el-icon><Document /></el-icon>
                    <el-icon><Compass /></el-icon>
                    <el-icon><Picture /></el-icon>
                    <el-icon><Microphone /></el-icon>
                </div>
            </light-card>
            <light-card style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px">
                <div v-for="item in topics.top" class="top-topic">
                    <el-tag type="info" size="small">置顶</el-tag>
                    <div>{{item.title}}</div>
                    <div>{{new Date(item.time).toLocaleDateString()}}</div>
                </div>
            </light-card>
            <light-card style="margin-top: 10px;display: flex;gap: 7px">
                <div :class="`type-select-card  ${topics.type === item.id ? 'active' : ''}`"
                     v-for="item in store.forum.types"
                     @click="topics.type = item.id">
                    <div class="type-icon" :style="{'background': item.color}"></div>
                    <span>{{item.name}}</span>
                </div>
            </light-card>
            <transition name="el-fade-in-linear" mode="out-in">
                <div v-if="topics.list.length">
                    <div v-infinite-scroll="updateList">
                        <light-card v-for="item in topics.list" class="topic-card"
                                    @click="router.push('/index/post-detail/' + item.id)">
                            <div style="display: flex">
                                <div>
                                    <el-avatar :size="30" :src="store.userAvatarUrl(item.avatar)"/>
                                </div>
                                <div style="margin-left: 7px;transform: translateY(-2px)">
                                    <div style="font-size: 13px;font-weight: bold">{{item.username}}</div>
                                    <div style="font-size: 12px;color: grey">
                                        <el-icon><Clock/></el-icon>
                                        <div style="margin-left: 2px;display: inline-block;transform: translateY(-2px)">{{new Date(item.time).toLocaleString()}}</div>
                                    </div>
                                </div>
                            </div>
                            <div style="margin-top: 5px">
                                <topic-tag :type="item.type"/>
                                <span style="margin-left: 7px;font-weight: bold">{{ item.title }}</span>
                            </div>
                            <div class="topic-preview-content">{{ item.text }}</div>
                            <div style="display: grid;grid-template-columns: repeat(3, 1fr);grid-gap: 10px">
                                <el-image v-for="img in item.images" fit="cover" :src="img" class="topic-image"></el-image>
                            </div>
                            <div style="display: flex;gap: 20px;font-size: 13px;margin-top: 10px">
                                <div>
                                    <el-icon style="vertical-align: middle"><CircleCheck /></el-icon> {{item.like}} 点赞
                                </div>
                                <div>
                                    <el-icon style="vertical-align: middle"><Star /></el-icon> {{item.collect}} 收藏
                                </div>
                            </div>
                        </light-card>
                    </div>
                </div>
            </transition>
        </div>
        <div style="width: 280px">
            <div style="position: sticky;top: 20px">
                <light-card>
                    <div class="collect-list-button" @click="collects = true">
                        <span><el-icon><FolderOpened /></el-icon> 查看我的收藏</span>
                        <el-icon style="transform: translateY(3px)"><ArrowRightBold/></el-icon>
                    </div>
                </light-card>
                <light-card style="margin-top: 10px">
                    <div style="font-weight: bold;">
                        <el-icon>
                            <CollectionTag/>
                        </el-icon>
                        论坛公告
                    </div>
                    <el-divider style="margin: 10px 0"/>
                    <div style="margin: 10px;font-size: 14px;color: grey">
                        为认真学习宣传贯彻党的二十大精神,深入贯彻习近平强军思想,
                        作为迎接办学70周年系列学术活动之一,国防科技大学将于2022年11月24日至26日在长沙举办“国防科技
                    </div>
                </light-card>
                <light-card style="margin-top: 10px">
                    <div style="font-weight: bold;">
                        <el-icon>
                            <Calendar/>
                        </el-icon>
                        天气信息
                    </div>
                    <el-divider style="margin: 10px 0"/>
                    <weather :data="weather"/>
                </light-card>
                <light-card style="margin-top: 10px">
                    <div style="display: flex;justify-content: space-between;color: grey;font-size: 14px">
                        <div>当前日期</div>
                        <div>{{ today }}</div>
                    </div>
                    <div style="display: flex;justify-content: space-between;color: grey;font-size: 14px">
                        <div>当前IP地址</div>
                        <div><b>127.0.0.1</b></div>
                    </div>
                </light-card>
                <div style="margin-top: 10px;color: grey;font-size: 14px">
                    <el-icon>
                        <Link/>
                    </el-icon>
                    友情链接
                    <el-divider style="margin: 10px 0"/>
                </div>
                <div style="display: grid;grid-template-columns: repeat(2, 1fr);grid-gap: 10px;margin-top: 10px">
                    <div class="friend-link">
                        <el-image src="https://element-plus.org/images/js-design-banner.jpg" style="height: 100%"/>
                    </div>
                    <div class="friend-link">
                        <el-image src="https://element-plus.org/images/vform-banner.png" style="height: 100%"/>
                    </div>
                    <div class="friend-link">
                        <el-image src="https://element-plus.org/images/sponsors/jnpfsoft.jpg" style="height: 100%"/>
                    </div>
                </div>
            </div>
        </div>
        <topic-editor :show="editor" @close="editor = false" @created="onTopicCreate"/>
        <topic-collect-list :show="collects" @close="collects = false"/>
    </div>
</template>

<style lang="less" scoped>
.collect-list-button {
    font-size: 14px;
    display: flex;
    justify-content: space-between;
    transition: .3s;

    &:hover {
        cursor: pointer;
        opacity: 0.6;
    }
}

.top-topic {
    display: flex;

    div:first-of-type {
        font-size: 14px;
        margin-left: 10px;
        font-weight: bold;
        opacity: 0.7;
        transition: color .3s;

        &:hover {
            color: grey;
        }
    }

    div:nth-of-type(2) {
        flex: 1;
        color: grey;
        font-size: 13px;
        text-align: right;
    }

    &:hover {
        cursor: pointer;
    }
}

.type-select-card {
    background-color: #f5f5f5;
    padding: 2px 7px;
    font-size: 14px;
    border-radius: 3px;
    box-sizing: border-box;
    transition: background-color .3s;

    .type-icon {
        display: inline-block;
        margin-right: 5px;
        border-radius: 5px;
        background: linear-gradient(to right, red, palegoldenrod);
        width: 10px;
        height: 10px;
    }

    &.active {
        border: solid 1px #ead4c4;
    }

    &:hover {
        cursor: pointer;
        background-color: #dadada;
    }
}

.topic-card {
    margin-top: 10px;
    transition: scale .3s;
    padding: 20px;

    &:hover {
        scale: 1.01;
        cursor: pointer;
    }

    .topic-image{
        width: 100%;
        height: 100%;
        max-height: 120px;
        border-radius: 5px;
    }

    .topic-preview-content {
        font-size: 13px;
        color: grey;
        margin: 10px 0;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 3;
        overflow: hidden;
        text-overflow: ellipsis;
    }
}


.friend-link {
    border-radius: 5px;
    overflow: hidden;
}

.edit-topic {
    background-color: #ececec;
    border-radius: 5px;
    height: 40px;
    color: grey;
    font-size: 14px;
    line-height: 40px;
    padding: 0 10px;

    :hover {
        cursor: pointer;
    }
}

.dark {
    .edit-topic {
        background-color: #282828;
    }

    .type-select-card {
        background-color: #282828;

        &.active {
            border: solid 1px #64594b;
        }

        &:hover {
            background-color: #5e5e5e;
        }
    }
}
</style>
