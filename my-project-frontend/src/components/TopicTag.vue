<script setup>

import {useStore} from "@/store";

const store = useStore()

defineProps({
    type: Number
})
</script>

<template>
    <div class="topic-type"
         :style="{color: store.findTypeById(type)?.color+'DD',
                         'border-color': store.findTypeById(type)?.color+'EE',
                         'background-color': store.findTypeById(type)?.color+'22'}">
        {{ store.findTypeById(type)?.name }}
    </div>
</template>

<style scoped>
.topic-type {
    display: inline-block;
    border: solid 0.5px grey;
    border-radius: 5px;
    font-size: 12px;
    padding: 0 5px;
    height: 18px;
}
</style>
