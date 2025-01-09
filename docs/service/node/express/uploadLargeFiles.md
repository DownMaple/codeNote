祖传开篇：本文简单介绍 怎么使用 express 实现大文件切片上传。

完整代码请前往 [gitHub仓库](https://github.com/DownMaple/express-upload-file)。

对应的 前端 文章请前往：[vue](../../../web/vue/components/chunkUpload)，
完整代码请前往：[vue GitHub仓库](https://github.com/DownMaple/maple-com/tree/master/src/uploadSharding)。

作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

## 环境要求

`node 18+`

需要的依赖如下：

```
  "type": "module",
  "dependencies": {
    "cors": "^2.8.5",
    "express": "^4.21.2",
    "multer": "1.4.5-lts.1",
    "nodemon": "^3.1.9"
  },
  "devDependencies": {
    "@types/node": "^22.10.5"
  }
```

## 目录结构

```
.
├─ router    路由文件夹  
│  ├─ router_handle   
│  │  └─ uploadChunk.js  路由详细方法
│  ├─ upload.js  路由注册页面
├─ uploads   文件服务器文件夹
└─ app.js    如果文件
└─ package.json
```

## 入口文件

启动一个 node 服务，用于监听请求。

```js
import express from "express";
import cors from "cors";
import uploadRouter from "./router/upload.js";

const app = express();
const PORT = process.env.PORT || 3000;

// 防止跨域问题
app.use(cors());
// 使用中间件解析JSON格式的请求体
app.use(express.json());

// 使用中间件解析URL编码的请求体，设置extended为false表示使用内置的querystring库解析
app.use(express.urlencoded({extended: false}));


// 静态文件托管
app.use('/uploads', express.static('uploads'));
// 设置路由
app.use('/upload', uploadRouter);


// 错误处理中间件
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({
    message: '错误',
    error: process.env.NODE_ENV === 'production' ? {} : err.message
  });
});

// 监听端口
app.listen(PORT, () => {
  console.log(`服务已经启动： http://localhost:${PORT}`);
});

```

## 路由注册

* chunk：切片文件上传接口
* merge：合并切片文件接口
* status：检查文件或切片是否存在

需要使用 `multer` 中间件，详细请看 [multer](https://github.com/expressjs/multer/blob/master/doc/README-zh-cn.md)

```js
import * as path from 'node:path';
import {fileURLToPath} from 'node:url';
import multer from 'multer'
import * as fs from 'node:fs';
import express from 'express';
import {fileExists, mergeChunk, uploadChunk} from './router_handle/uploadChunk.js';
// 创建一个路由器实例
const router = express.Router()
// 获取当前文件的路径
const __filename = fileURLToPath(import.meta.url);
// 获取当前文件所在的目录路径
const __dirname = path.dirname(__filename);
// 定义上传目录路径
const UPLOAD_DIR = path.join(__dirname, '../uploads');
// 如果上传目录不存在，则创建上传目录
if (!fs.existsSync(UPLOAD_DIR)) {
  fs.mkdirSync(UPLOAD_DIR);
}

/**
 * 配置 multer 进行文件切片上传
 *
 * @param {Object} req - Express 请求对象，用于获取文件哈希
 * @param {Object} file - 当前正在处理的文件对象
 * @param {Function} cb - 回调函数，用于确定文件的存储位置和文件名
 */
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    // 从请求体中获取文件哈希值
    const {fileHash} = req.body;
    // 构建文件切片的目录路径
    const chunkDir = path.join(UPLOAD_DIR, fileHash);
    // 如果文件切片目录不存在，则创建该目录
    if (!fs.existsSync(chunkDir)) {
      fs.mkdirSync(chunkDir, {recursive: true});
    }
    // 设置文件切片的存储目录
    cb(null, chunkDir);
  },
  filename: (req, file, cb) => {
    // 从请求体中获取文件切片的哈希
    const {chunkHash} = req.body;
    // 设置文件切片的名称
    cb(null, `chunk-${chunkHash}`);
  }
});
// 初始化 multer 中间件
const upload = multer({storage});

// 分片上传接口
router.post('/chunk', upload.single('file'), uploadChunk);

// 合并上传文件
router.post('/merge', mergeChunk);

// 检查分片是否完成上传
router.post('/status', fileExists);

export default router;

```

`upload.single('file')` 的作用：

* 指定字段名：'file' 是表单中文件字段的 name 属性值。
* 自动处理文件：multer 会自动将文件保存到指定位置（磁盘或内存），并将文件信息挂载到 req.file。

## 路由详细方法

### 切片上传方法

因为在 设置路由的时候通过 `upload.single('file')` 已经处理了文件，所以这里只需要获取文件信息即可。

```js
export const uploadChunk = async (req, res) => {
  
  if (!req.file) {
    return res.status(500).json({message: '没有文件上传', code: 500});
  }
  
  res.send({
    data: req.body,
    message: '切片上传完成',
    code: 200
  });
};

```

### 检验文件和切片

```js
/**
 * 检查文件是否存在，检验文件切片是否存在
 * @param req
 * @param res
 * @returns {Promise<*>}
 */
export const fileExists = async (req, res) => {
      const {fileName, fileHash, chunkHash} = req.body
      
      const filePath = path.join(UPLOAD_DIR, fileName); // 最终文件路径
      const chunkDir = path.join(UPLOAD_DIR, fileHash); // 分片文件夹路径
      
      try {
        // 检查文件是否存在
        if (fs.existsSync(filePath)) {
          const existingFileHash = calculateFileHash(filePath);
          // 如果哈希值一致则不用上传
          if (existingFileHash === fileHash) {
            return res.send({
              code: 200,
              data: {
                fileName,
                pathUrl: `/uploads/${fileName}`
              },
              message: '文件已存在'
            });
          } else {
            // 如果不一致则更改本次上传的文件名称，通知前端继续上传
            // 如果文件哈希值不一致，生成新的文件名
            const nameArr = fileName.split('.');
            let baseName, extension;
            
            if (nameArr.length > 1) {
              baseName = nameArr.slice(0, -1).join('.');
              extension = nameArr[nameArr.length - 1];
            } else {
              baseName = fileName;
              extension = '';
            }
            
            // 生成新的文件名
            const newFileName = `${baseName}-${Date.now()}.${extension}`
            return res.send({
              code: 200,
              data: {
                fileName: newFileName
              },
              message: '文件名重复，已修改'
            })
          }
        }
        
        // 检查是否用该哈希值的文件夹
        if (!fs.existsSync(chunkDir)) {
          return res.send({
            code: 200,
            message: '允许上传',
            data: []
          });
        }
        
        // 如果文件夹存在，则对比子文件和 chunkHash , 返回已经上传的切片的哈希值
        const chunks = fs.readdirSync(chunkDir);
        const uploadedChunks = chunks
            .filter((chunk) => chunk.startsWith('chunk-'))
            .map((chunk) => chunk.split('-')[1]); // 提取分片的哈希值
        
        return res.send({
          code: 200,
          data: {
            uploadedChunks
          },
          message: '分片已存在'
        })
      
      } catch (e) {
        console.error('检查分片状态出错：', e);
        return res.status(500).send({
          code: 500,
          message: '服务器错误',
          error: e.message
        });
      }
    };

/**
 * 计算文件的哈希值
 * @param filePath
 * @returns {string}
 */
function calculateFileHash(filePath) {
  const fileBuffer = fs.readFileSync(filePath);
  return crypto.createHash('md5').update(fileBuffer).digest('hex');
}


```

### 合并文件

```js
/**
 * 合并文件
 * @param req
 * @param res
 * @returns {Promise<*>}
 */
export const mergeChunk = async (req, res) => {
      const {fileName, fileHash, totalChunks, chunk} = req.body;
      const chunkDir = path.join(UPLOAD_DIR, fileHash);
      const finalPath = path.join(UPLOAD_DIR, fileName);
      
      try {
        // 读取分片目录中的所有文件
        const chunks = await fs.promises.readdir(chunkDir)
        // 过滤并排序分片，确保它们按正确顺序合并
        const sortedChunks = sortFileChunk(chunks.filter(file => file.startsWith('chunk-')), chunk)
        // 如果分片数量不匹配，返回错误响应
        if (sortedChunks.length !== totalChunks) {
          return res.send({
            message: '分片不完整',
            code: 500
          });
        }
        // 使用流式方式合并文件
        const writeStream = fs.createWriteStream(finalPath, {flags: 'a'});
        for (const chunk of sortedChunks) {
          const chunkPath = path.join(chunkDir, chunk);
          
          // 异步检查文件是否存在
          await fs.promises.access(chunkPath, fs.constants.F_OK);
          // 创建读取流并合并到最终文件中
          const readStream = fs.createReadStream(chunkPath);
          
          await new Promise((resolve, reject) => {
            readStream.pipe(writeStream, {end: false}); // 不自动结束写入流
            readStream.on('end', resolve);
            readStream.on('error', reject);
          });
          // 删除已经合并的分片
          await fs.promises.unlink(chunkPath);
        }
        
        
        // 结束写入
        writeStream.end();
        
        // 删除分片文件夹
        await fs.promises.rmdir(chunkDir);
        
        return res.send({
          message: '文件上传成功',
          data: {
            fileName,
            pathUrl: `/uploads/${fileName}`
          },
          code: 200
        });
      
      } catch (error) {
        console.error('合并出现错误：', error);
        return res.status(500).send({
          message: '合并文件失败',
          error: error.message,
          code: 500
        });
      }
    };

/**
 * 对文件切片进行排序
 * @param fileList
 * @param chunk
 * @returns {*[]}
 */
function sortFileChunk(fileList, chunk) {
  let chunkList = []
  let chunkSort = chunk.sort((a, b) => a.index - b.index)
  chunkSort.forEach(item => {
    let fileItem = fileList.find(file => file.startsWith(`chunk-${item.fileHash}`))
    chunkList.push(fileItem)
  })
  return chunkList
}
```
