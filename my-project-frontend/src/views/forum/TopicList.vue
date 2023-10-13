<script setup>
import LightCard from "@/components/LightCard.vue";
import {Calendar, Clock, CollectionTag, EditPen, Link} from "@element-plus/icons-vue";
import Weather from "@/components/Weather.vue";
import {computed, reactive, ref} from "vue";
import {get} from "@/net";
import {ElMessage} from "element-plus";
import TopicEditor from "@/components/TopicEditor.vue";
import {useStore} from "@/store";
import axios from "axios";

const store = useStore()

const weather = reactive({
    location: {},
    now: {},
    hourly: [],
    success: false
})
const editor = ref(false)
const list = ref(null)

const today = computed(() => {
    const date = new Date()
    return `${date.getFullYear()} 年 ${date.getMonth() + 1} 月 ${date.getDate()} 日`
})
get('/api/forum/types', data => store.forum.types = data)
function updateList(){
    get('/api/forum/list-topic?page=0&type=0', data => list.value = data)
}
updateList()

navigator.geolocation.getCurrentPosition(position => {
    const longitude = position.coords.longitude
    const latitude = position.coords.latitude
    get(`/api/forum/weather?longitude=${longitude}&latitude=${latitude}`, data => {
        Object.assign(weather, data)
        weather.success = true
    })
}, error => {
    console.info(error)
    ElMessage.warning('位置信息获取超时，请检测网络设置')
    get(`/api/forum/weather?longitude=116.40529&latitude=39.90499`, data => {
        Object.assign(weather, data)
        weather.success = true
    })
}, {
    timeout: 3000,
    enableHighAccuracy: true
})
</script>

<template>
    <div style="display: flex;margin: 20px auto;gap: 20px;max-width: 900px">
        <div style="flex: 1">
            <light-card>
                <div class="create-topic" @click="editor = true">
                    <el-icon><EditPen/></el-icon> 点击发表主题...
                </div>
            </light-card>
            <light-card style="margin-top: 10px;height: 30px">
            </light-card>
            <div style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px">
                <light-card v-for="item in list" class="topic-card">
                    <div style="display: flex">
                        <div>
                            <el-avatar :size="30" :src="`${axios.defaults.baseURL}/images${item.avatar}`"/>
                        </div>
                        <div style="margin-left: 7px;transform: translateY(-2px)">
                            <div style="font-size: 13px;font-weight: bold">{{item.username}}</div>
                            <div style="font-size: 12px;color: grey">
                                <el-icon><Clock/></el-icon>
                                <div style="margin-left: 2px;display: inline-block;transform: translateY(-2px)">
                                    {{new Date(item.time).toLocaleString()}}
                                </div>
                            </div>
                        </div>
                    </div>
                    <div style="margin-top: 5px">
                        <div class="topic-type"
                             :style="{
                                color: store.findTypeById(item.type)?.color + 'EE',
                                'border-color': store.findTypeById(item.type)?.color + '77',
                                'background': store.findTypeById(item.type)?.color + '33'
                             }">
                            {{store.findTypeById(item.type)?.name}}
                        </div>
                        <span style="font-weight: bold;margin-left: 7px">{{item.title}}</span>
                    </div>
                    <div class="topic-content">{{item.text}}</div>
                    <div style="display: grid;grid-template-columns: repeat(3, 1fr);grid-gap: 10px">
                        <el-image class="topic-image" v-for="img in item.images" :src="img" fit="cover"></el-image>
                    </div>
                </light-card>
            </div>
        </div>
        <div style="width: 280px">
            <div style="position: sticky;top: 20px">
                <light-card>
                    <div style="font-weight: bold">
                        <el-icon><CollectionTag/></el-icon>
                        论坛公告
                    </div>
                    <el-divider style="margin: 10px 0"/>
                    <div style="font-size: 14px;margin: 10px;color: grey">
                        为认真学习宣传贯彻党的二十大精神,深入贯彻习近平强军思想,
                        作为迎接办学70周年系列学术活动之一,国防科技大学将于2022年11月24日至26日在长沙举办“国防科技
                    </div>
                </light-card>
                <light-card style="margin-top: 10px">
                    <div style="font-weight: bold">
                        <el-icon><Calendar/></el-icon>
                        天气信息
                    </div>
                    <el-divider style="margin: 10px 0"/>
                    <weather :data="weather"/>
                </light-card>
                <light-card style="margin-top: 10px">
                    <div class="info-text">
                        <div>当前日期</div>
                        <div>{{today}}</div>
                    </div>
                    <div class="info-text">
                        <div>当期IP地址</div>
                        <div>127.0.0.1</div>
                    </div>
                </light-card>
                <div style="font-size: 14px;margin-top: 10px;color: grey">
                    <el-icon><Link/></el-icon>
                    友情链接
                    <el-divider style="margin: 10px 0"/>
                </div>
                <div style="display: grid;grid-template-columns: repeat(2, 1fr);grid-gap: 10px;margin-top: 10px">
                    <div class="friend-link">
                        <el-image style="height: 100%" src="https://element-plus.org/images/js-design-banner.jpg"/>
                    </div>
                    <div class="friend-link">
                        <el-image style="height: 100%" src="https://element-plus.org/images/vform-banner.png"/>
                    </div>
                    <div class="friend-link">
                        <el-image style="height: 100%" src="https://element-plus.org/images/sponsors/jnpfsoft.jpg"/>
                    </div>
                </div>
            </div>
        </div>
        <topic-editor :show="editor" @success="editor = false;updateList()" @close="editor = false"/>
    </div>
</template>

<style lang="less" scoped>
.topic-card {
    padding: 15px;
    transition: scale .3s;

    &:hover {
        scale: 1.015;
        cursor: pointer;
    }

    .topic-content {
        font-size: 13px;
        color: grey;
        margin: 5px 0;
        display: -webkit-box;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 3;
        overflow: hidden;
        text-overflow: ellipsis;
    }

    .topic-type {
        display: inline-block;
        border: solid 0.5px grey;
        border-radius: 3px;
        font-size: 12px;
        padding: 0 5px;
        height: 18px;
    }

    .topic-image {
        width: 100%;
        height: 100%;
        max-height: 110px;
        border-radius: 5px;
    }
}

.info-text {
    display: flex;
    justify-content: space-between;
    color: grey;
    font-size: 14px;
}

.friend-link {
    border-radius: 5px;
    overflow: hidden;
}

.create-topic {
    background-color: #efefef;
    border-radius: 5px;
    height: 40px;
    color: grey;
    font-size: 14px;
    line-height: 40px;
    padding: 0 10px;

    &:hover {
        cursor: pointer;
    }
}

.dark .create-topic {
    background-color: #232323;
}
</style>
