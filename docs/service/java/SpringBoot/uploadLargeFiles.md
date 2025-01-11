使用 ` SpringBoot ` 实现大文件切片上传。

完整代码请前往 [gitHub仓库](https://github.com/DownMaple/upload-file-java)。

对应的 前端 文章请前往：[vue](../../../web/vue/components/chunkUpload)，
完整代码请前往：[vue GitHub仓库](https://github.com/DownMaple/maple-com/tree/master/src/uploadSharding)。

祖传开篇：作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

## 准备工作

springBoot 的 WebMvcConfigurer 和 HandlerInterceptor 的实现就不提了， 依赖环境如下：

关键点：

* SpringBoot 版本：我们使用的是 3.4.1 版本。
* Java 版本：项目要求 Java 17 环境。
* Lombok：用于简化代码，减少样板代码的编写。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.1</version>
        <relativePath/> <!-- lookup parent from repository -->
    </parent>
    <groupId>com.file</groupId>
    <artifactId>upload-file-java</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>upload-file-java</name>
    <description>upload-file-java</description>
    <url/>
    <licenses>
        <license/>
    </licenses>
    <developers>
        <developer/>
    </developers>
    <scm>
        <connection/>
        <developerConnection/>
        <tag/>
        <url/>
    </scm>
    <properties>
        <java.version>17</java.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>


        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.36</version>
            <optional>true</optional>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>

</project>

```

## 切片上传

实现切片上传的接口

### DAO

首先，我们需要定义一个数据传输对象（DTO），用于接收前端传递的切片信息。

```java
package com.file.uploadfilejava.file.DAO;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class UploadChunkDAO {
    private String fileName;      // 文件名称
    private int chunkIndex;       // 切片索引
    private String chunkHash;     // 切片文件哈希值
    private String fileHash;      // 文件哈希值
    private MultipartFile file;   // 切片文件
}

```

### controller

接下来，我们在控制器层定义一个接口，用于接收前端上传的切片文件。

```java
package com.file.uploadfilejava.file.Controller;

import com.file.uploadfilejava.file.DAO.UploadChunkDAO;
import com.file.uploadfilejava.file.DAO.UploadMergeDAO;
import com.file.uploadfilejava.file.DAO.UploadStatusDAO;
import com.file.uploadfilejava.file.Service.FileService;
import com.file.uploadfilejava.utils.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/upload")
public class FileController {

    @Resource
    private FileService fileService;

    //    切片上传接口
    @PostMapping("/chunk")
    public R uploadChunk(UploadChunkDAO uploadChunkData) {
        try {
            return fileService.uploadFileChunk(uploadChunkData);
        } catch (IOException e) {
            return R.error().message("切片上传失败");
        }

    }

}

```

### service

服务层负责具体的业务逻辑，包括保存切片文件到指定目录。

```java
package com.file.uploadfilejava.file.Service;

import com.file.uploadfilejava.file.DAO.UploadChunkDAO;
import com.file.uploadfilejava.file.DAO.UploadMergeDAO;
import com.file.uploadfilejava.file.DAO.UploadStatusDAO;
import com.file.uploadfilejava.utils.R;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.*;


import static com.file.uploadfilejava.utils.CalculateFileHash.calculateFileHash;

@Service
public class FileService {

    // 静态上传目录
    public static String uploadDirStatic = "uploads/";

    /**
     * 上传文件分片
     * 该方法负责处理文件上传过程中的分片接收和保存操作它根据文件的哈希值确定存储目录，
     * 并确保目录存在如果目录不存在，它会创建必要的目录结构然后，它根据分片的哈希值生成
     * 分片文件名，并将分片数据写入到对应的文件中
     *
     * @param uploadChunkDAO 包含上传分片信息和数据的对象，包括文件哈希、分片哈希和文件内容
     * @return 返回上传结果的响应对象，包含上传状态和消息
     * @throws IOException 如果文件写入过程中发生I/O错误
     */
    public R uploadFileChunk(UploadChunkDAO uploadChunkDAO) throws IOException {
        // 构造上传目录路径，基于静态上传目录和文件哈希值
        String uploadDir = uploadDirStatic + uploadChunkDAO.getFileHash();
        Path dirPath = Paths.get(uploadDir);
        // 检查上传目录是否存在，如果不存在则创建
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }

        // 构造分片文件名，基于分片哈希值
        String chunkFileName = "chunk-" + uploadChunkDAO.getChunkHash();

        // 获取分片文件的完整路径
        Path chunkFilePath = dirPath.resolve(chunkFileName);

        // 从上传分片信息中获取文件
        MultipartFile file = uploadChunkDAO.getFile();

        // 检查文件是否存在
        if (file != null && !file.isEmpty()) {
            Files.write(chunkFilePath, file.getBytes());
            return R.ok().message("文件分片上传成功");
        } else {
            return R.error().message("未获取到文件");
        }
    }

}
 
```

## 文件检查

为了确保上传过程的完整性，我们需要实现文件和切片的存在性检查。

### DAO

定义一个数据传输对象，用于接收文件和切片的状态查询请求

```java
package com.file.uploadfilejava.file.DAO;

import lombok.Data;

import java.util.ArrayList;

@Data
public class UploadStatusDAO {
    private String fileName;    // 文件名称
    private String fileHash;    // 文件哈希值
    private ArrayList<String> chunkHash;    /// 切片哈希值列表
}

```

### controller

在控制器层定义一个接口，用于处理文件和切片的状态查询请求。

```java 
package com.file.uploadfilejava.file.Controller;

import com.file.uploadfilejava.file.DAO.UploadChunkDAO;
import com.file.uploadfilejava.file.DAO.UploadMergeDAO;
import com.file.uploadfilejava.file.DAO.UploadStatusDAO;
import com.file.uploadfilejava.file.Service.FileService;
import com.file.uploadfilejava.utils.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/upload")
public class FileController {

    @Resource
    private FileService fileService;


    //    切片和文件状态查询接口
    @PostMapping("/status")
    public R uploadStatus(@RequestBody UploadStatusDAO uploadStatusDAO) {
        try {
            return fileService.uploadFileStatus(uploadStatusDAO);
        } catch (NoSuchAlgorithmException | IOException e) {
            return R.error().message("检验失败");
        }
    }

}

```

### service

服务层负责具体的业务逻辑，包括文件和切片的存在性检查。

```java
 package com.file.uploadfilejava.file.Service;

import com.file.uploadfilejava.file.DAO.UploadChunkDAO;
import com.file.uploadfilejava.file.DAO.UploadMergeDAO;
import com.file.uploadfilejava.file.DAO.UploadStatusDAO;
import com.file.uploadfilejava.utils.R;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.*;


import static com.file.uploadfilejava.utils.CalculateFileHash.calculateFileHash;

@Service
public class FileService {

    public static String uploadDirStatic = "uploads/";


    // 检查文件是否存在，检验文件切片是否存在
    public R uploadFileStatus(UploadStatusDAO uploadStatusDAO) throws IOException, NoSuchAlgorithmException {

        String uploadDir = uploadDirStatic + uploadStatusDAO.getFileName();
        Path dirPath = Paths.get(uploadDir);

        // 判断文件是否存在
        if (Files.exists(dirPath)) {
            // 通过自定义方法计算文件哈希值
            String fileHash = calculateFileHash(dirPath);
            //  如果文件存在，则判断文件哈希是否相同
            if (fileHash.equals(uploadStatusDAO.getFileHash())) {
                return R.ok().message("文件已存在").data("fileName", uploadStatusDAO.getFileName()).data("pathUrl", dirPath.toString());
            }
            // 文件哈希不同，处理文件名冲突
            String[] nameArray = uploadStatusDAO.getFileName().split("\\.");
            String baseName, extension;
            if (nameArray.length > 1) {
                // 通过 Arrays.asList() 方法将数组转换为列表，然后使用 subList() 方法获取数组的前 n-1 个元素，并使用 join() 方法将这些元素连接成一个字符串。
                List<String> nameList = Arrays.asList(nameArray).subList(0, nameArray.length - 1);
                baseName = String.join(".", nameList);
                extension = nameArray[nameArray.length - 1];
            } else {
                baseName = uploadStatusDAO.getFileName();
                extension = "";
            }
            // 生成新的文件名， 就是在 文件名后面加一个时间戳
            String newFileName = baseName + "-" + System.currentTimeMillis() + "." + extension;

            return R.ok().message("文件名重复，已修改").data("fileName", newFileName);

        } else {

            // 文件不存在，检查文件切片目录
            String chunkDir = uploadDirStatic + uploadStatusDAO.getFileHash();
            Path chunkDirPath = Paths.get(chunkDir);
            // 如果切片目录不存在，则直接开始上传切片
            if (!Files.exists(chunkDirPath)) {
                return R.ok().message("允许上传");
            }

            // 如果文件夹存在，则对比子文件和 chunkHash, 返回已经上传的切片的哈希值
            List<String> chunks = Files.list(chunkDirPath)
                    .filter(path -> path.getFileName().toString().startsWith("chunk-"))
                    .map(path -> path.getFileName().toString().split("-")[1])
                    .toList();

            // 返回已上传的切片哈希值
            return R.ok().message("分片已存在").data("uploadedChunks", chunks);

        }
    }

}

 ```

### 计算哈希值

```java 
package com.file.uploadfilejava.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CalculateFileHash {

    /**
     * 计算文件的哈希值
     * 该方法读取文件内容，并使用MD5算法计算文件的哈希值
     * 哈希值可用于验证文件的完整性
     *
     * @param filePath 文件的路径，用于定位文件
     * @return 返回文件的MD5哈希值的十六进制字符串表示
     * @throws IOException              如果文件读取过程中发生错误
     * @throws NoSuchAlgorithmException 如果指定的哈希算法不可用
     */
    public static String calculateFileHash(Path filePath) throws IOException, NoSuchAlgorithmException {
        // 创建MD5哈希算法的MessageDigest实例 ， 算法要和前端保持一致
        MessageDigest digest = MessageDigest.getInstance("MD5");

        // 使用try-with-resources语句自动管理文件流的关闭
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            // 创建DigestInputStream以在读取文件内容时自动更新哈希值
            DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest);
            byte[] buffer = new byte[8192];
            while (digestInputStream.read(buffer) > 0) {
                // 读取文件内容以计算哈希值
                // DigestInputStream 会自动更新 MessageDigest 的状态
            }
        }
        // 完成哈希计算，得到哈希值的字节数组
        byte[] hashBytes = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            // 将字节 b 转换为无符号整数，并将其转换为十六进制字符串。
            String hex = Integer.toHexString(0xff & b);
            //检查生成的十六进制字符串是否只有一位。如果是，则说明该字节的高四位为零，需要补全为两位。
            if (hex.length() == 1) {
                // 如果确实是单个字符，则先添加一个 '0'，以确保最终结果是两位十六进制数。
                hexString.append('0');
            }
            // 最后将实际的十六进制字符添加到 StringBuilder 中。
            hexString.append(hex);
        }

        return hexString.toString();
    }
}

```

## 合并切片

当所有切片上传完成后，我们需要将这些切片合并成一个完整的文件。

### DAO

定义一个数据传输对象，用于接收切片合并的请求信息。

```java 
package com.file.uploadfilejava.file.DAO;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;

@Data
public class UploadMergeDAO {
    private String fileName;    // 文件名称
    private String fileHash;    // 文件哈希值
    private int totalChunks;    // 切片总数
    private ArrayList<HashMap<String, String>> chunk;    // 切片信息 ：包含切片哈希值和切片索引
}

```

### controller

在控制器层定义一个接口，用于处理切片合并的请求。

```java 
package com.file.uploadfilejava.file.Controller;

import com.file.uploadfilejava.file.DAO.UploadChunkDAO;
import com.file.uploadfilejava.file.DAO.UploadMergeDAO;
import com.file.uploadfilejava.file.DAO.UploadStatusDAO;
import com.file.uploadfilejava.file.Service.FileService;
import com.file.uploadfilejava.utils.R;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("/upload")
public class FileController {

    @Resource
    private FileService fileService;

    //    文件切片合并接口
    @PostMapping("/merge")
    public R uploadMerge(@RequestBody UploadMergeDAO uploadMergeDAO) {
        try {
            return fileService.uploadFileMerge(uploadMergeDAO);
        } catch (IOException e) {
            return R.error().message("合并文件失败");
        }
    }
}

```

### service

服务层负责具体的业务逻辑，包括切片的排序和合并。

```java 
package com.file.uploadfilejava.file.Service;

import com.file.uploadfilejava.file.DAO.UploadChunkDAO;
import com.file.uploadfilejava.file.DAO.UploadMergeDAO;
import com.file.uploadfilejava.file.DAO.UploadStatusDAO;
import com.file.uploadfilejava.utils.R;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.*;


import static com.file.uploadfilejava.utils.CalculateFileHash.calculateFileHash;

@Service
public class FileService {

    public static String uploadDirStatic = "uploads/";

    /**
     * 处理文件上传和合并的逻辑
     * 该方法负责将多个分片文件合并成一个完整的文件
     *
     * @param uploadMergeDAO 包含上传合并所需信息的数据访问对象
     * @return 返回一个表示上传结果的R对象，包括错误信息或上传成功的数据
     * @throws IOException 如果文件操作失败，抛出IOException
     */
    public R uploadFileMerge(UploadMergeDAO uploadMergeDAO) throws IOException {
        String uploadDir = uploadDirStatic + uploadMergeDAO.getFileHash();
        Path dirPath = Paths.get(uploadDir);

        // 检查上传目录是否存在
        if (!Files.exists(dirPath)) {
            return R.error().message("合并文件失败");
        }

        // 读取分片目录中的所有文件
        List<Path> chunks = Files.list(dirPath)
                .filter(path -> path.getFileName().toString().startsWith("chunk-"))
                .toList();

        // 检查分片数量是否匹配
        if (chunks.size() != uploadMergeDAO.getTotalChunks()) {
            return R.error().message("分片不完整");
        }

        // 对分片进行排序
        List<Path> sortedChunks = sortFileChunk(chunks, uploadMergeDAO.getChunk());

        // 合并文件
        Path finalFilePath = Paths.get(uploadDirStatic + uploadMergeDAO.getFileName()); // 使用配置的上传目录

        // 使用输出流合并分片文件
        try (OutputStream outputStream = Files.newOutputStream(finalFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (Path chunk : sortedChunks) {
                Files.copy(chunk, outputStream);
                Files.delete(chunk); // 删除已合并的分片
            }
        }

        // 删除分片目录
        Files.delete(dirPath);

        return R.ok().message("文件上传成功").data("pathUrl", finalFilePath.toString()).data("fileName", uploadMergeDAO.getFileName());
    }

    /**
     * 对文件分片进行排序
     *
     * @param chunks     分片文件路径列表
     * @param chunkInfos 分片信息列表
     * @return 排序后的分片文件路径列表
     */
    private List<Path> sortFileChunk(List<Path> chunks, ArrayList<HashMap<String, String>> chunkInfos) {
        return chunkInfos.stream()
                .sorted(Comparator.comparingInt(info -> Integer.parseInt(info.get("index")))) // 根据 chunkInfos 列表中的 index 字段对分片信息进行排序
                .map(info -> chunks.stream()
                        .filter(chunk -> chunk.getFileName().toString().startsWith("chunk-" + info.get("fileHash"))) // 对于每个排序后的分片信息，找到对应的分片文件路径。
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("分片未找到: " + info.get("fileHash")))) // 如果找不到对应的分片文件，则抛出异常。
                .toList();
    }
}

```

## 结尾

通过上述步骤，我们成功实现了基于 SpringBoot 的大文件切片上传功能。该方案不仅提高了上传的可靠性，还增强了用户体验。

### 参考资料

[Spring Boot 官方文档](https://spring.io/projects/spring-boot/)
[Lombok 官方文档](https://projectlombok.org/)

