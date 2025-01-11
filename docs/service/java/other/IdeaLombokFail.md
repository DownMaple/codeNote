祖传开篇：没错，Idea 的问题。作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

## 问题

问题场景就是在 实体中，使用 @Data 注解

```
package com.file.uploadfilejava.file;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;



@Data
public class UploadChunkDAO {
    private String fileName;
    private int chunkIndex;
    private String chunkHash;
    private String fileHash;
    private MultipartFile file;
}

```

在代码中能够正常使用，且会有代码提示，启动或者构建项目时会报错，如下：

```bash
java: 找不到符号
D:·········省略·············filelfileController.iava:12:42
符号:方法 getFileName()
位置:类型为com.file.uploadfilejava.file.UploadchunkDA0的变量 uploadchunkData
```

提示找不到！为啥？我代码提示都出来了，你给我说没有？！

## 解决方法

### 第一步

打开idea设置：

1. 设置
2. 构建、执行、部署
3. 编译器
4. 注解处理器
5. 在这里 勾选 `启用注解处理`
6. 然后选择 `从项目路径获取处理器`
7. 点击应用，然后确定

### 第二部

打开插件，搜索 `Lombok` 然取消勾选（就是给卸载掉），然后重启 idea， 重新安装 `Lombok` 插件。

## 结尾

我是这样弄好的，可能大家遇到的错误和我的不太一样导致无法解决，那就抱歉无法给您带来帮助了
