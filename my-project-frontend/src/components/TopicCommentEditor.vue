<script setup>
import {Delta, QuillEditor} from "@vueup/vue-quill";
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import {post} from "@/net";
import {ref} from "vue";
import {ElMessage} from "element-plus";

const props = defineProps({
    quote: Object,
    show: Boolean,
    tid: String
})
const emit = defineEmits(['close', 'comment'])

const content = ref()

function init() {
    content.value = new Delta()
}

function submitComment() {
    if(deltaToText(content).length > 2000) {
        ElMessage.warning('评论字数长度已经超出最大限制，请缩减评论内容！')
        return
    }
    post('/api/forum/add-comment', {
        tid: props.tid,
        content: JSON.stringify(content.value),
        quote: props.quote == null ? -1 : props.quote.id
    }, () => {
        ElMessage.success('评论成功')
        emit('comment')
    })
}

function deltaToText(delta) {
    if(!delta?.ops) return ""
    let str = ''
    for (let ops of delta.ops) {
        str += ops.insert
    }
    return str.replace(/\s/g, "")
}

function deltaToSimpleText(delta) {
    let str = deltaToText(JSON.parse(delta))
    if(str.length > 35) str = str.substring(0, 35) + "..."
    return str
}
</script>

<template>
    <div>
        <el-drawer :model-value="show"
                   :title="quote ? `发表对评论: ${deltaToSimpleText(quote.content)} 的回复` : '发表帖子回复'"
                   @open="init"
                   :size="270" direction="btt"
                   @close="emit('close')"
                   :show-close="false">
            <div style="padding: 10px;width: 100%">
                <div>
                    <quill-editor style="height: 100px" v-model:content="content"
                                  placeholder="请发表友善的评论，不要使用脏话骂人，都是大学生素质高一点"/>
                </div>
                <div style="margin-top: 10px;display: flex">
                    <div style="flex: 1;font-size: 13px;color: grey">
                        字数统计: {{deltaToText(content).length}}（最大支持2000字）
                    </div>
                    <el-button @click="submitComment" type="success" plain>发表评论</el-button>
                </div>
            </div>
        </el-drawer>
    </div>
</template>

<style lang="less" scoped>
:deep(.el-drawer) {
    width: 800px;
    margin: 20px auto;
    border-radius: 10px;
}
:deep(.el-drawer__header) {
    margin: 0;
}
:deep(.el-drawer__body) {
    padding: 10px;
}
</style>
