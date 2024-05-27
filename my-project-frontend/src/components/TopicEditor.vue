<script setup>
import {Document} from "@element-plus/icons-vue";
import {QuillEditor, Quill, Delta} from "@vueup/vue-quill";
import {computed, reactive, ref} from "vue";
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import {accessHeader, post} from "@/net";
import axios from "axios";
import {ElMessage} from "element-plus";
import ImageResize from "quill-image-resize-vue";
import { ImageExtend, QuillWatch } from "quill-image-super-solution-module";
import {useStore} from "@/store";

const props = defineProps({
    show: Boolean,
    defaultTitle: {
        default: '',
        type: String
    },
    defaultText: {
        default: '',
        type: String
    },
    defaultType: {
        default: 1,
        type: Number
    },
    submitButton: {
        default: '立即发布',
        type: String
    },
    submit: {
        default: (editor, success) => {
            post('/api/forum/create-topic', {
                type: editor.type,
                title: editor.title,
                content: editor.text
            }, () => {
                ElMessage.success("帖子发表成功！")
                success()
            })
        },
        type: Function
    }
})
const emit = defineEmits(['close', 'created'])
const store = useStore()

const refEditor = ref()
const editor = reactive({
    type: null,
    title: '',
    text: '',
    uploading: false,
})

function initEditor() {
    if(props.defaultText)
        editor.text = new Delta(JSON.parse(props.defaultText))
    else
        refEditor.value.setContents('', 'user')
    editor.title = props.defaultTitle
    editor.type = props.defaultType
}

function submitTopic(){
    const text = deltaToText(editor.text)
    if(text.length > 20000) {
        ElMessage.warning('字数超出限制，无法发布主题！')
        return
    }
    if(!editor.title) {
        ElMessage.warning('请填写标题！')
        return
    }
    if(!editor.type) {
        ElMessage.warning('请选择一个合适的帖子类型！')
        return
    }
    props.submit(editor, () => emit('created'))
}

function deltaToText(delta) {
    if(!delta.ops) return ""
    let str = ''
    for (let ops of delta.ops) {
        str += ops.insert
    }
    return str.replace(/\s/g, "")
}

const contentLength = computed(() => deltaToText(editor.text).length)

Quill.register('modules/imageResize', ImageResize);
Quill.register('modules/ImageExtend', ImageExtend)
const editorOption = {
    placeholder: "今天分享点什么呢？",
    modules: {
        toolbar: {
            container: [
                "bold", "italic", "underline", "strike","clean",
                {color: []}, {'background': []},
                {size: ["small", false, "large", "huge"]},
                { header: [1, 2, 3, 4, 5, 6, false] },
                {list: "ordered"}, {list: "bullet"}, {align: []},
                "blockquote", "code-block", "link", "image",
                { indent: '-1' }, { indent: '+1' }
            ],
            handlers: {
                'image': function () {
                    QuillWatch.emit(this.quill.id)
                }
            }
        },
        imageResize: {
            modules: [ 'Resize', 'DisplaySize' ]
        },
        ImageExtend: {
            action:  axios.defaults.baseURL + '/api/image/cache',
            name: 'file',
            size: 5,
            loading: true,
            accept: 'image/png, image/jpeg',
            response: (resp) => {
                if(resp.data) {
                    return axios.defaults.baseURL + '/images' + resp.data
                } else {
                    return null
                }
            },
            methods: 'POST',
            headers: xhr => {
                xhr.setRequestHeader('Authorization', accessHeader().Authorization);
            },
            start: () => editor.uploading = true,
            success: () => {
                ElMessage.success('图片上传成功!')
                editor.uploading = false
            },
            error: () => {
                ElMessage.warning('图片上传失败，请联系管理员!')
                editor.uploading = false
            }
        }
    }
}
</script>

<template>
    <div>
        <el-drawer direction="btt" :model-value="show"
                   :size="650" @open="initEditor"
                   @close="emit('close')"
                   :show-close="false">
            <template #header>
                <div>
                    <div style="font-weight: bold">发表新的帖子</div>
                    <div style="font-size: 13px">发表您的内容之前，请遵守相关法律法规，不要出现骂人爆粗口这种不文明行为。</div>
                </div>
            </template>
            <div style="display: flex;gap: 10px">
                <div style="width: 120px">
                    <el-select placeholder="选择类型..." v-model="editor.type" :disabled="!store.forum.types.length">
                        <el-option v-for="item in store.forum.types" :value="item.id" :label="item.name"/>
                    </el-select>
                </div>
                <div style="flex: 1">
                    <el-input maxlength="30" placeholder="请输入帖子标题..."
                              :prefix-icon="Document" v-model="editor.title"/>
                </div>
            </div>
            <div style="margin-top: 15px;height: 450px;border-radius: 5px;overflow: hidden"
                 v-loading="editor.uploading"
                 element-loading-text="正在上传图片，请稍后...">
                <quill-editor ref="refEditor" v-model:content="editor.text"
                              :options="editorOption" content-type="delta"
                              style="height: calc(100% - 44px)"/>
            </div>
            <div style="display: flex;justify-content: space-between;margin-top: 10px">
                <div style="color: grey;font-size: 13px">
                    当前字数: {{ contentLength }}（最大支持20000字）
                </div>
                <div>
                    <el-button @click="submitTopic" type="success" plain>{{submitButton}}</el-button>
                </div>
            </div>
        </el-drawer>
    </div>
</template>

<style lang="less" scoped>
:deep(.el-drawer) {
    width: 800px;
    margin: auto;
    border-radius: 10px 10px 0 0;
}
:deep(.el-drawer__header) {
    margin: 0;
}
</style>
