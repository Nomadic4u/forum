<script setup>
import Card from "@/components/Card.vue";
import {Lock, Setting, Switch} from "@element-plus/icons-vue";
import {reactive, ref} from "vue";
import {get, post} from "@/net";
import {ElMessage} from "element-plus";

const form = reactive({
    password: '',
    new_password: '',
    new_password_repeat: '',
})

const validatePassword = (rule, value, callback) => {
    if (value === '') {
        callback(new Error('请再次输入密码'))
    } else if (value !== form.new_password) {
        callback(new Error("两次输入的密码不一致"))
    } else {
        callback()
    }
}

const rules = {
    password: [
        { required: true, message: '请输入原来的密码', trigger: 'blur' },
    ],
    new_password: [
        { required: true, message: '请输入新的密码', trigger: 'blur' },
        { min: 6, max: 16, message: '密码的长度必须在6-16个字符之间', trigger: ['blur'] }
    ],
    new_password_repeat: [
        { required: true, message: '请重复输入新的密码', trigger: 'blur' },
        { validator: validatePassword, trigger: ['blur', 'change'] },
    ]
}

const formRef = ref()
const valid = ref(false)
const onValidate = (prop, isValid) => valid.value = isValid

function resetPassword() {
    formRef.value.validate((isValid) => {
        if(isValid) {
            post('/api/user/change-password', form, () => {
                ElMessage.success('密码修改成功！')
                formRef.value.resetFields()
            })
        }
    })
}

const privacy = reactive({
    phone: false,
    email: false,
    wx: false,
    qq: false,
    gender: false
})
get('/api/user/privacy', data => {
    privacy.phone = data.phone
    privacy.email = data.email
    privacy.wx = data.wx
    privacy.qq = data.qq
    privacy.gender = data.gender
    saving.value = false
})
const saving = ref(true)
function savePrivacy(type, status) {
    saving.value = true
    post('/api/user/save-privacy', {
        type: type,
        status: status
    }, () => {
        ElMessage.success('隐式设置保存成功！')
        saving.value = false
    })
}
</script>

<template>
    <div style="margin: auto;max-width: 600px">
        <div style="margin-top: 20px">
            <card :icon="Setting" title="隐私设置" v-loading="saving"
                  desc="在这里设置哪些内容可以被其他人看到，请各位小伙伴注重自己的隐私">
                <div class="checkbox-list">
                    <el-checkbox @change="savePrivacy('phone', privacy.phone)"
                                 v-model="privacy.phone">公开展示我的手机号</el-checkbox>
                    <el-checkbox @change="savePrivacy('email', privacy.email)"
                                 v-model="privacy.email">公开展示我的电子邮件地址</el-checkbox>
                    <el-checkbox @change="savePrivacy('wx', privacy.wx)"
                                 v-model="privacy.wx">公开展示我的微信号</el-checkbox>
                    <el-checkbox @change="savePrivacy('qq', privacy.qq)"
                                 v-model="privacy.qq">公开展示我的QQ号</el-checkbox>
                    <el-checkbox @change="savePrivacy('gender', privacy.gender)"
                                 v-model="privacy.gender">公开展示我的性别</el-checkbox>
                </div>
            </card>
            <card style="margin: 20px 0"
                  :icon="Setting" title="修改密码" desc="修改密码请在这里进行操作，请务必牢记您的密码">
                <el-form @validate="onValidate" :model="form" :rules="rules"
                         ref="formRef" style="margin: 20px" label-width="100">
                    <el-form-item label="当前密码" prop="password">
                        <el-input type="password" v-model="form.password"
                                  :prefix-icon="Lock" placeholder="当前密码" maxlength="16"/>
                    </el-form-item>
                    <el-form-item label="新密码" prop="new_password">
                        <el-input type="password" v-model="form.new_password"
                                  :prefix-icon="Lock" placeholder="新密码" maxlength="16"/>
                    </el-form-item>
                    <el-form-item label="重复新密码" prop="new_password_repeat">
                        <el-input type="password" v-model="form.new_password_repeat"
                                  :prefix-icon="Lock" placeholder="重复新密码" maxlength="16"/>
                    </el-form-item>
                    <div style="text-align: center">
                        <el-button :icon="Switch" @click="resetPassword"
                                   type="success" :disabled="!valid">立即重置密码</el-button>
                    </div>
                </el-form>
            </card>
        </div>
    </div>
</template>

<style scoped>
.checkbox-list {
    margin: 0 0 20px 10px;
    display: flex;
    flex-direction: column;
}
</style>
