<template>
  <div class="file-upload-container">
    <el-upload
      ref="uploadRef"
      :action="uploadUrl"
      :headers="headers"
      :multiple="multiple"
      :limit="limit"
      :accept="accept"
      :file-list="fileList"
      :auto-upload="autoUpload"
      :show-file-list="showFileList"
      :drag="drag"
      :list-type="listType"
      :on-preview="handlePreview"
      :on-remove="handleRemove"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-progress="handleProgress"
      :before-upload="handleBeforeUpload"
      :on-exceed="handleExceed"
    >
      <template v-if="drag">
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          将文件拖到此处，或<em>点击上传</em>
        </div>
      </template>
      <template v-else>
        <el-button type="primary">
          <el-icon><Upload /></el-icon>
          {{ buttonText }}
        </el-button>
      </template>
      <template #tip>
        <div v-if="tip" class="el-upload__tip">{{ tip }}</div>
      </template>
    </el-upload>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Upload } from '@element-plus/icons-vue'
import type { UploadFile, UploadFiles, UploadInstance } from 'element-plus'
import { useUserStore } from '@/stores/user'

interface Props {
  uploadUrl?: string
  multiple?: boolean
  limit?: number
  accept?: string
  fileList?: UploadFile[]
  autoUpload?: boolean
  showFileList?: boolean
  drag?: boolean
  listType?: 'text' | 'picture' | 'picture-card'
  buttonText?: string
  tip?: string
  maxSize?: number // MB
}

const props = withDefaults(defineProps<Props>(), {
  uploadUrl: `${import.meta.env.VITE_API_BASE_URL}/api/upload`,
  multiple: true,
  limit: 10,
  accept: '',
  fileList: () => [],
  autoUpload: true,
  showFileList: true,
  drag: false,
  listType: 'text',
  buttonText: '选择文件',
  tip: '',
  maxSize: 10
})

interface Emits {
  (e: 'success', response: any, file: UploadFile, fileList: UploadFiles): void
  (e: 'error', error: Error, file: UploadFile, fileList: UploadFiles): void
  (e: 'remove', file: UploadFile, fileList: UploadFiles): void
  (e: 'preview', file: UploadFile): void
  (e: 'progress', event: { percent: number }, file: UploadFile, fileList: UploadFiles): void
}

const emit = defineEmits<Emits>()

const uploadRef = ref<UploadInstance>()
const userStore = useUserStore()

const headers = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

const handleBeforeUpload = (file: File) => {
  // 检查文件大小
  const isLt = file.size / 1024 / 1024 < props.maxSize
  if (!isLt) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB!`)
    return false
  }

  // 检查文件类型
  if (props.accept) {
    const fileTypes = props.accept.split(',').map(type => type.trim())
    const fileType = file.type
    const fileName = file.name
    const fileExt = fileName.substring(fileName.lastIndexOf('.')).toLowerCase()

    const isValidType = fileTypes.some(type => {
      if (type.startsWith('.')) {
        return fileExt === type.toLowerCase()
      }
      if (type.includes('*')) {
        return fileType.match(new RegExp(type.replace('*', '.*')))
      }
      return fileType === type
    })

    if (!isValidType) {
      ElMessage.error(`只能上传 ${props.accept} 格式的文件!`)
      return false
    }
  }

  return true
}

const handleSuccess = (response: any, file: UploadFile, fileList: UploadFiles) => {
  if (response.code === 200) {
    ElMessage.success('上传成功')
    emit('success', response, file, fileList)
  } else {
    ElMessage.error(response.message || '上传失败')
    emit('error', new Error(response.message), file, fileList)
  }
}

const handleError = (error: Error, file: UploadFile, fileList: UploadFiles) => {
  ElMessage.error('上传失败')
  emit('error', error, file, fileList)
}

const handleRemove = (file: UploadFile, fileList: UploadFiles) => {
  emit('remove', file, fileList)
}

const handlePreview = (file: UploadFile) => {
  emit('preview', file)
}

const handleProgress = (event: { percent: number }, file: UploadFile, fileList: UploadFiles) => {
  emit('progress', event, file, fileList)
}

const handleExceed = () => {
  ElMessage.warning(`最多只能上传 ${props.limit} 个文件`)
}

// 暴露方法
defineExpose({
  submit: () => {
    uploadRef.value?.submit()
  },
  abort: () => {
    uploadRef.value?.abort()
  },
  clearFiles: () => {
    uploadRef.value?.clearFiles()
  }
})
</script>

<style scoped>
.file-upload-container {
  width: 100%;
}

:deep(.el-upload-dragger) {
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

:deep(.el-upload-dragger:hover) {
  border-color: #409eff;
}

:deep(.el-icon--upload) {
  font-size: 67px;
  color: #c0c4cc;
  margin-bottom: 16px;
  line-height: 50px;
}

:deep(.el-upload__text) {
  color: #606266;
  font-size: 14px;
  text-align: center;
}

:deep(.el-upload__text em) {
  color: #409eff;
  font-style: normal;
}

:deep(.el-upload__tip) {
  font-size: 12px;
  color: #606266;
  margin-top: 7px;
}
</style>