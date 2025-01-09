本文只是介绍关于 rust 的 rocket 框架中怎么实现一个断点续传的接口，作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

# 什么情况下需要使用

* 大文件下载：因为下载周期长，如果中途断开，需要重新下载，如果使用断点续传，可以避免重新下载
* 网络不稳定：下载文件时可能频繁中断
* 多线程下载：断点续传技术也常用于多线程下载中，将文件分成多个部分并行下载。每个部分从指定的字节范围开始下载，从而加速整体下载速度。如果某个部分失败，也只需要重新下载失败的部分。
* 优化用户体验：当用户中断了下载后，能够继续下载未完成的部分提供了更好的体验

<hr/>

# 实现步骤

下面是一个简单的实现，具体实现方式可能不同，这里只是个参考。

## 在main中，添加路由

```rust
#[rocket::main]
async fn main() -> Result<(), rocket::Error> {
    let _rocket = rocket::build()
        // 需要过滤的路由（在静态文件路由之前，实现某些文件的处理）
        .mount("/", file::create_routes())   
        // 静态文件路由
        .mount("/static", FileServer::from(relative!("static")))  
        .register("/", catchers![not_found, unprocessable_entity])
        .manage(connect_to_db().await)
        .launch()
        .await?;

    Ok(())
}
```

## 创建路由文件，添加路由接口

```rust

use alloc::vec::Vec;
use rocket::{routes, Route};
use crate::models::file::file_controller::download_file;

pub fn create_routes() -> Vec<Route> {
    routes![
        download_file,
    ]
}

```

## 最后，实现接口

使用 rocket 的 自定义请求守卫，获取 range 参数，并处理该参数。
这是所有要导入的依赖

```rust
use std::io::SeekFrom;
use rocket::{get, response, Response};
use std::path::{Path, PathBuf};
use rocket::http::{ContentType, Status};
use rocket::response::{Responder};
use tokio::fs::File;
use rocket::request::{FromRequest, Request};
use rocket::outcome::Outcome;
use tokio::io::{AsyncReadExt, AsyncSeekExt};
```

### 实现路由守卫

```rust
pub struct RangeHeader {
    start: Option<u64>,
    end: Option<u64>,
}

#[rocket::async_trait]
impl<'r> FromRequest<'r> for RangeHeader {
    type Error = ();

    async fn from_request(request: &'r Request<'_>) -> rocket::request::Outcome<Self, Self::Error> {
        if let Some(range) = request.headers().get("Range").next() {
            println!("Range: {}", range);
            if let Some(range_str) = range.strip_prefix("bytes=") {
                let parts: Vec<&str> = range_str.split('-').collect();
                let start = parts.get(0).and_then(|s| s.parse::<u64>().ok());
                let end = parts.get(1).and_then(|s| s.parse::<u64>().ok());

                return Outcome::Success(RangeHeader { start, end });
            }
        }
        Outcome::Forward(Status::new(200))
    }
}
```

### 实现接口功能

```rust
#[get("/static/music/<file_path..>")]
pub async fn download_file(file_path: PathBuf, range: RangeHeader) -> Result<FileResponse, Status> {

    // 读取文件 (每个人处理文件的方式可能不同，这里是简单的demo)
    let file_path = Path::new("static/music/").join(file_path);
    let mut file = File::open(&file_path).await.map_err(|_| Status::NotFound).expect("File not found");
    let metadata = file.metadata().await.map_err(|_| Status::InternalServerError).expect("Failed to get file metadata");
    let file_size = metadata.len();

    // 获取 Range 信息
    let (range_start, range_end) = match (range.start, range.end) {
        (Some(start), Some(end)) => (start, end),
        (Some(start), None) => (start, file_size - 1),
        (None, Some(end)) => (0, end),
        (None, None) => (0, file_size - 1),
    };

    // 读取文件的字节范围
    let mut buffer = vec![0; (range_end - range_start + 1) as usize];
    file.seek(SeekFrom::Start(range_start)).await.map_err(|_| Status::InternalServerError)?;
    file.read_exact(&mut buffer).await.map_err(|_| Status::InternalServerError)?;

    // 返回自定义的响应守卫
    Ok(FileResponse {
        buffer,
        range_start,
        range_end,
        file_size,
    })
}

```

### 定义响应守卫

```rust
pub struct FileResponse {
    buffer: Vec<u8>,
    range_start: u64,
    range_end: u64,
    file_size: u64,
}
impl<'r> Responder<'r, 'static> for FileResponse {
    fn respond_to(self, req: &'r Request<'_>) -> response::Result<'static> {

        let string = format!("{}:{}:{}", self.range_start, self.range_end, self.file_size);
        let range_str = format!("bytes {}-{}/{}", self.range_start, self.range_end, self.file_size);
        Response::build_from(string.respond_to(req)?)
            .raw_header("Accept-Ranges", "bytes")
            .raw_header("Content-Range", range_str)
            .header(ContentType::new("application", "x-person"))  // 测试使用
            .status(Status::Ok)
            .sized_body(self.buffer.len(), std::io::Cursor::new(self.buffer))
            .ok()
    }
}
```

## 接口请求示例

请求头信息（部分）：

| 请求头    | 参数                  |
|--------|---------------------|
| Range  | bytes=440000-450000 |
| Accept | */*                 |
| ···    | ···                 |

响应头信息（部分）：

| 响应头            | 参数                            |
|----------------|-------------------------------|
| content-type   | application/x-person          |
| accept-ranges  | 	bytes                        |
| content-range  | 	bytes 440000-450000/44012654 |
| server         | Rocket                        |
| content-length | 10001                         |
| ···            | ···                           |

实际响应（demo）：��R��X��HN[3z��<�Y�����M&��w��······（省略）
