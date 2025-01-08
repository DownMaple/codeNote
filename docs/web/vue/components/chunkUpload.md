祖传开篇：本文简单介绍 怎么使用 elementPlus 上传组件 和 web worker 实现大文件切片上传。

完整代码请前往 [gitHub仓库]()。

对应的 Node Express 服务端文章请前往：[node]()， 完整代码请前往：[node GitHub仓库]()。

对应的 java SpringBoot 服务端文章请前往：[java]()，完整代码请前往：[java GitHub仓库]()。

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


const uploadFileValue = ref<UploadUserFile[]>([])   // 绑定上传文件列表 ，用于在组件展示
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
