祖传开篇：本文简单介绍 怎么使用 elementPlus 上传组件 和 web worker 实现大文件切片上传。

完整代码请前往 [gitHub仓库](https://github.com/DownMaple/maple-com/tree/master/src/uploadSharding)。

对应的 Node Express 服务端文章请前往：[node](../../../service/node/express/uploadLargeFiles)，
完整代码请前往：[node GitHub仓库](https://github.com/DownMaple/express-upload-file)。

对应的 java SpringBoot 服务端文章请前往：[java](../../../service/java/SpringBoot/uploadLargeFiles)
，完整代码请前往：[java GitHub仓库](https://github.com/DownMaple/upload-file-java)。

作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

## 环境要求

* node 18+
* vue 3.5+
* elementPlus 2.8+
* sass 1.8+
* typescript 5.5+
* vite 5.0+

## 创建组件

为了不写样式和部分交互，所以直接使用 elementPlus 的上传组件。

暂只支持单文件，多文件可以自行设置实现。

```vue

<template>
  <el-upload
      ref="uploadRef"
      accept="*/*"
      :auto-upload="false" 关闭自动上传
      :file-list="uploadFileValue" 绑定上传文件列表
      :on-change="uploadFile" 当文件发生变化时触发
      :on-remove="uploadRemove" 当文件被移除时触发
      :on-preview="uploadPreview" 当上传后的文件被点击时触发
  >
    <template #default>
      <slot slot="default"> 创建一个默认插槽，可以自定义上传按钮
        <el-button type="primary">选择文件</el-button>
      </slot>
    </template>

    <template #tip> 创建一个tip插槽，可以自定义提示信息
      <div class="el-upload__tip">
        {{ tip }}
      </div>
    </template>
  </el-upload>
</template>
```

## 选择文件

就不详细阐述了，直接看代码

```ts
import {UploadUserFile} from 'element-plus';
import {FileItemType} from './utils/type.ts';
import {ref, toRaw} from 'vue';

// 绑定上传文件列表 ，用于在组件展示
const uploadFileValue = defineModel({
  type: Array as PropType<UploadUserFile[]>,
  required: true,
  default: () => []
})


// 懂的都懂
const {uploadUrl, testHashUrl, mergeFileUrl, downloadFileUrl, size, fileData} = defineProps({
  uploadUrl: String,        // 上传地址
  testHashUrl: String,      // hash检验地址
  mergeFileUrl: String,     // 合并文件地址
  downloadFileUrl: String,  // 下载地址
  size: {                   // 切片大小
    type: Number,
    default: 5 * 1024 * 1024
  },
  tip: {                    // 提示信息
    type: String,
    default: '请选择上传的文件'
  },
  fileData: Object          // 合并文件的时候附加信息 
})

const fileValue = ref<FileItemType | null>(null)  // 文件信息用于文件操作

function uploadFile(file: UploadUserFile) {
  uploadFileValue.value = [file]
  if (file && file.raw) {
    generateFile(file.raw, size).then(res => {   // 调用 文件切片的方法，参数为 文件、切片大小 
      fileValue.value = res                      // 将文件切片信息赋值给 fileValue
      uploadFileValue.value[0].status = 'ready'  // 将上传状态改为 ready
      beginUploadFile()                          // 调用上传文件的方法
    })
  }
}
```

定义文件类型： utils/type.ts

```ts
export interface FileItemType {
  hash: string;
  file: File;
  name: string;
  size: number;
  loaded: number
  progress: number
  status: string;
  response: any
  chunkNum: number
  pathUrl: string
  chunks: ChunkType[]
  uploadedIndexList: number[]
}

export interface ChunkType {
  index: number;
  fileName: string;
  fileHash: string;
  start: number;
  end: number;
  total: number;
  chunk: Blob;
  chunkNum: number
}

```

## 通过 web worker 生成文件切片

前面都是开胃菜，这里才是重点，通过 `web worker` 在子线程中 生成文件切片 和 计算文件的 hash 值，避免占用主线程太多资源导致其他操作阻塞

[web worker](../../base/JavaScript/webWorker#importscripts) 笔记

先在 `utils` 文件下 创建一个 `generateFile.ts`

```ts
import type {FileItemType} from './type.ts';
import {UploadRawFile} from 'element-plus/es/components/upload/src/upload';

export function generateFile(file: UploadRawFile, chunkSize = 1024 * 1024 * 5) {
  return new Promise<FileItemType>((resolve) => {
    // 创建一个 web worker, 通过 new URL() 引入 worker 文件
    const worker = new Worker(new URL('./generateFile.worker.js', import.meta.url));
    // 监听 worker 线程的消息
    worker.onmessage = function (event) {
      resolve(event.data);
      worker.terminate();
    };
    // 将文件、切片大小、uid 发送给 worker现成，worker 接收到消息开始执行
    worker.postMessage({file, chunkSize, uid: file.uid});
  });
}

```

接着在 `utils` 文件下 创建一个 `generateFile.worker.js`

```js
// 重点，这个文件要放到本地，引入需要使用importScripts()  
importScripts("spark-md5.js")    // 引入 spark-md5.js 
self.onmessage = async function (event) {
  
  const {file, chunkSize, uid} = event.data;
  const chunkNum = Math.ceil(file.size / chunkSize);
  
  const fileItem = {
    fid: uid,   // 没啥用的 id
    file,       // 文件对象
    name: file.name, // 文件名称
    size: file.size, // 文件大小
    loaded: 0,       // 已上传大小（好像没用到）
    progress: 0,     // 上传进度
    fileHash: await calculateHash(file),    // 整个文件的哈希值
    status: "create", // 初始化文件状态，这里用create表示已创建
    response: {},     // 上传请求的响应数据存放在这里
    chunkNum,         // 切片数量
    pathUrl: "",      // 上传后的地址
    chunks: [],       // 这里存放切片数组
    uploadedIndexList: [] // 这里是已上传的切片索引
  }
  let index = 0;
  let start = 0;
  // 根据 设置的 切片大小，循环生成切片数组
  while (start < file.size) {
    let end = start + chunkSize;
    if (end > file.size) end = file.size;
    
    const chunk = {
      index,    // 切片索引
      fileName: file.name,  // 文件名称
      fileHash: '',   // 切片的哈希值
      start,  // 切片开始位置
      end,    // 切片结束位置
      total: file.size,   // 文件总大小
      chunk: file.slice(start, end), // 调用 slice 方法进行文件切割
      chunkNum   // 切片数量
    };
    chunk.fileHash = await calculateHash(chunk.chunk);   // 计算切片的哈希值
    fileItem.chunks.push(chunk);
    start += chunkSize;
    index++;
  }
  self.postMessage(fileItem);    // 完成切片后，将文件对象发送给主线程
};

// 异步计算hash值
function calculateHash(chunk) {
  return new Promise((resolve, reject) => {
    // 初始化一个 FileReader 对象用于读取文件
    const reader = new FileReader();
    // 当文件读取成功后，计算哈希值
    reader.onloadend = function () {
      // 使用 SparkMD5 库计算 ArrayBuffer 的 MD5 哈希值
      const hash = SparkMD5.ArrayBuffer.hash(reader.result);
      resolve(hash); // 返回计算后的哈希值
    };
    // 如果文件读取过程中出现错误，拒绝 Promise 并返回错误信息
    reader.onerror = function (error) {
      reject(error); // 出错时拒绝
    };
    // 读取切片为 ArrayBuffer
    reader.readAsArrayBuffer(chunk);
  });
}

```

这样我们就会得到在worker线程中返回文件对象，示例：
![An image](/image/web/chunkUpload-file.png)

## 切片上传

因为要频繁的和ui交互，所以将 切片上传 的逻辑放在 组件文件中，

### 组件中

```ts

function beginUploadFile() {
  if (fileValue.value && uploadFileValue.value.length > 0) {
    // 开始上传文件，将文件状态改为上传中
    fileValue.value.status = 'uploading'
    uploadFileValue.value[0].status = 'uploading'
    
    // 新建 worker 线程， 直接在组件中创建，是因为要 频繁的和 ui 进行交互
    worker = new Worker(new URL('./utils/uploadFile.worker.js', import.meta.url));
    
    // 监听 worker 消息
    worker.onmessage = function (event) {
      const {type, message} = event.data;
      
      // 根据消息类型，做出处理
      switch (type) {
          
          // 当文件上传成功，将文件状态改为成功，同时将文件的 网络路径 保存到 fileValue 中，用于点击下载
        case 'success': {
          if (fileValue.value) {
            fileValue.value.status = 'success'
            uploadFileValue.value[0].status = 'success'
            fileValue.value.pathUrl = event.data.data.pathUrl
            showSuccess(`文件：${event.data.data.fileName} 上传成功`)   //  注意，这里是简单的 ElementPlus 的提示封装
            // 源码
            // /**
            //  * 成功弹出
            //  * @param msg
            //  */
            // export const showSuccess = (msg: string = "成功") => {
            //   ElMessage(
            //       {
            //         message: msg,
            //         type: 'success'
            //       }
            //   )
            // }
          }
        }
          break;
          
          // 当文件上传失败
        case 'error': {
          if (fileValue.value) {
            fileValue.value.status = 'fail'
            uploadFileValue.value[0].status = 'fail'
          } else {
            showError(message)
          }
        }
          break;
          
          // 当文件上传进度发生变化
        case 'uploadProgress': {
          updateUploadProgress(event.data)
        }
          break;
          
          // 当文件名称相同，哈希值不同，则需要更改文件名称上传（名称由服务端返回）
        case 'updateFileName': {
          if (fileValue.value) {
            fileValue.value.name = event.data.data.fileName
            uploadFileValue.value[0].name = event.data.data.fileName
          }
        }
          break;
          
          // 其他
        default: {
          console.log(event)
        }
      }
    };
    
    // 监听线程错误
    worker.onerror = function (error) {
      showError(`Worker error: ${error.message}`);
    };
    
    // 发送文件处理消息，让线程开始工作
    worker.postMessage(
        {
          type: 'generateFile',
          file: toRaw(fileValue.value),
          fileData,
          uploadUrl,
          testHashUrl,
          mergeFileUrl
        });
  }
}

```

### worker 中

> [!TIP]
> 请求 node-express 的文件上传接口时，一定要注意， file 文件 作为最后的参数上传

```js
self.onmessage = async function (event) {
  const {type, file, fileDate, uploadUrl, testHashUrl, mergeFileUrl} = event.data;
  if (type === 'generateFile') {
    // 文件上传逻辑
    await uploadFile(file, fileDate, uploadUrl, testHashUrl, mergeFileUrl)
  } else if (type === 'terminate') {
    self.postMessage({
      type: 'error',
      message: '上传终止'
    })
  } else {
    console.log('Unknown message type:', type);
  }
  // 释放线程
  self.close();
}

async function uploadFile(file, fileDate, uploadUrl, testHashUrl, mergeFileUrl) {
  // 先检验文件是否存在，和切片上传状态
  const testResult = await request(testHashUrl, {
    fileName: file.name,
    fileHash: file.fileHash,
    chunkHash: file.chunks.map(item => item.fileHash)
  })
  if (testResult.code === 200) {
    // 当文件存在，则直接返回文件地址
    if (testResult.message === '文件已存在') {
      self.postMessage({
        type: 'success',
        message: '文件已存在',
        data: testResult.data
      })
      return
    } else if (testResult.message === '文件名重复，已修改') {
      // 当文件同名，但是哈希值不同，则修改文件名，继续上传
      self.postMessage({
        type: 'updateFileName',
        message: '文件名重复，已修改',
        data: testResult.data
      })
      file.name = testResult.data.fileName
    }
  } else {
    // 上传失败
    self.postMessage({
      type: 'error',
      message: '上传失败，请检查网络连接或联系开发人员',
      error: testResult.error
    })
    return
  }
  
  // 根据文件检验结果，获取需要上传的切片
  const chunksToUpload = testResult.data.length > 0 ? file.chunks.filter(chunk => {
    return testResult.data.includes(chunk.fileHash)
  }) : file.chunks
  
  // 如果没有需要上传的切片，且文件不存在，则直接合并文件
  if (chunksToUpload.length === 0) {
    const mergeRes = await mergeFile(file, mergeFileUrl)
    if (mergeRes.code === 200) {
      self.postMessage({
        type: 'success',
        message: '文件完成上传',
        data: mergeRes.data
      })
    } else {
      self.postMessage({
        type: 'error',
        message: '文件合并失败'
      })
    }
    return
  }
  
  const uploadRequestList = requestMap(chunksToUpload, file.name, file.fileHash, uploadUrl)
  // 等待所有切片上传完成
  const allResult = await Promise.all(uploadRequestList);
  
  // 判断是否有切片上传失败，如果上传失败，则尝试重新上传一次
  if (allResult.some(item => item.code !== 200)) {
    // 这里不确定错误的会不会返回错误码，所以这里判断正确的
    const indexList = allResult.map((item, index) => {
      if (item.code === 200) {
        return index
      }
    })
    // 获取再次上传的切片列表
    const againUploadList = chunksToUpload.map(item => !indexList.indexOf(item.index))
    // 创建上传请求
    const againUploadRequestList = requestMap(againUploadList, file.name, file.fileHash, uploadUrl)
    const againAllResult = await Promise.all(againUploadRequestList);
    if (againAllResult.some(item => item.code !== 200)) {
      self.postMessage({
        type: 'error',
        message: '上传失败，请检查网络连接或联系开发人员'
      })
      return
    }
  }
  // 如果全部完成，则准备合并文件
  const mergeRes = await mergeFile(file, mergeFileUrl, fileDate)
  if (mergeRes.code === 200) {
    self.postMessage({
      type: 'success',
      message: '文件完成上传',
      data: mergeRes.data
    })
  } else {
    self.postMessage({
      type: 'error',
      message: '文件合并失败'
    })
  }
}

/**
 * 发送请求
 * @param url 请求地址
 * @param data 请求数据
 * @param method 请求方法
 * @param headers 请求头
 * @returns {Promise<unknown>}
 */
function request(url, data, method = 'POST', headers = {'Content-Type': 'application/json'}) {
  return new Promise((resolve, reject) => {
    fetch(url, {
      method: method,
      headers: headers,
      body: JSON.stringify(data)
    }).then(res => res.json())
        .then(response => {
          resolve(response)
        })
        .catch(err => {
          reject(err)
        })
  })
}

/**
 * 请求映射
 * @param chunksList
 * @param fileName
 * @param fileHash
 * @param uploadUrl
 * @returns {*}
 */
function requestMap(chunksList, fileName, fileHash, uploadUrl) {
  return chunksList.map(chunk => {
    const formData = new FormData();
    formData.append('fileName', fileName);     // 文件名
    formData.append('chunkIndex', chunk.index);  // 当前切片的索引
    formData.append('chunkHash', chunk.fileHash);  // 当前切片的哈希值
    formData.append('fileHash', fileHash);     // 文件哈希值
    formData.append('file', chunk.chunk);    // 切片文件 (如果请求的node，记得放最后)
    return fetch(uploadUrl, {
      method: 'POST',
      body: formData // 使用 FormData 上传文件
    })
        .then(res => res.json())
        .then(response => {
          self.postMessage({
            type: 'uploadProgress',
            message: '切片上传成功',
            data: {
              chunkIndex: response.data.chunkIndex,
              chunkHash: response.data.fileHash
            }
          })
          return response
        }).catch(err => {
          return err
        });
  })
}

/**
 * 合并文件请求
 * @param file
 * @param mergeFileUrl
 * @param fileDate
 * @returns {Promise<unknown>}
 */
function mergeFile(file, mergeFileUrl, fileDate) {
  return new Promise((resolve, reject) => {
    fetch(mergeFileUrl, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        fileName: file.name,      // 文件名称
        fileHash: file.fileHash,     // 文件哈希值
        totalChunks: file.chunkNum,   // 切片总数
        chunk: file.chunks.map(item => {  // 切片哈希数组
          return {fileHash: item.fileHash, index: item.index}
        }),
        ...fileDate
      })
    }).then(res => res.json())
        .then(response => {
          console.log(response)
          resolve(response)
        })
        .catch(err => {
          reject(err)
        })
  })
}

```

## 其他交互

```ts

/**
 * 移除文件
 * @param _        被移除的文件
 * @param fileList  移除后的列表
 */
function uploadRemove(_: UploadUserFile, fileList: UploadUserFile[]) {
  uploadFileValue.value = fileList
  // 如果在上传过程中，移除了文件，则终止线程
  if (worker) {
    worker!.postMessage({type: 'terminate', file: ''});
    fileValue.value = null
  }
}

/**
 * 更新上传进度
 * @param data
 */
function updateUploadProgress(data: any) {
  if (fileValue.value && uploadFileValue.value.length > 0) {
    fileValue.value.uploadedIndexList.push(data.data.chunkIndex)
    // 简单的使用切片数量计算上传进度，也可以用文件大小计算进度
    fileValue.value.progress = Number(((fileValue.value.uploadedIndexList.length / fileValue.value.chunkNum) * 100).toFixed(1))
    // 修改上传组件的进度展示
    uploadFileValue.value[0].percentage = fileValue.value.progress
  }
}

/**
 * 预览文件，直接点击下载文件
 */
function uploadPreview() {
  if (fileValue.value) {
    if (fileValue.value.status === 'success') {
      // 简简单单一个文件下载
      dowLoadFile({}, downloadFileUrl + fileValue.value.pathUrl, fileValue.value.name)
    } else {
      showWarn('请等待文件上传完成')
    }
  }
}

```

`packageUtils.ts` 中的文件下载

```ts

/**
 * blob 文件流的方式下载文件
 * @param {string} url    下载文件的路径
 * @param {object} data    下载文件需要的参数
 * @param fileName         文件名称
 * @param method           请求方式
 * @param headers         文件下载时的请求头
 */
export async function dowLoadFile(data: any, url: string, fileName: string, method: string = 'GET', headers?: {
  key: string,
  value: string
}) {
  const xhr = new XMLHttpRequest()
  xhr.open(method, url, true)
  xhr.setRequestHeader('Content-Type', 'application/json')
  if (headers) {
    Object.entries(headers).forEach(([key, value]) => {
      xhr.setRequestHeader(key, value)
    })
  }
  xhr.responseType = 'blob'
  xhr.onload = () => {
    const blob = xhr.response
    // const blobUrl = URL.createObjectURL(blob)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = fileName
    a.style.display = 'none'
    a.click()
  }
  xhr.send(JSON.stringify(data))
}

```

## 使用组件

```vue

<script setup lang="ts">
  import UploadSharding from './uploadSharding/uploadSharding.vue';
  import {ref, watchEffect} from 'vue';

  const fileList = ref([])

  watchEffect(() => {
    console.log(fileList.value)
  })
</script>

<template>
  <div>
    <upload-sharding
        v-model="fileList"
        test-hash-url="http://localhost:3000/upload/status"
        upload-url="http://localhost:3000/upload/chunk"
        merge-file-url="http://localhost:3000/upload/merge"
        download-file-url="http://localhost:3000"
    >
    </upload-sharding>
  </div>
</template>

<style scoped>
</style>
```

## 效果

![An image](/image/web/chunkUpload-audio.gif)
