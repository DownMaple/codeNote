本文主要介绍，通过 ```sysinfo = "0.33.0"``` 库获取
cpu、内存、磁盘、网络使用情况。[文档入口](https://docs.rs/sysinfo/0.33.0/sysinfo/)

作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。

当前环境： rustup 1.27.1 (54dd3d00f 2024-04-24)

## 安装依赖

```
[build-dependencies]
sysinfo = "0.33.0"
serde = { version = "1", features = ["derive"] }
tokio = "1.42.0"
```

这里我们将磁盘信息和内存等信息分开获取。
因为内存、cpu、网络 等信息需要与上一次获取的值做比较。
而磁盘信息可以直接获取到数据。

## 获取硬盘信息

### 声明结构体 ###

这里返回值是一个结构体，包含磁盘名称、驱动器盘符、总空间和可用空间。

```rust
use serde::{Deserialize, Serialize};
#[derive(Debug,Serialize, Deserialize)]
pub struct DisksInfo {
    name: String,
    drive_letter: String,
    total_space: u64,
    available_space: u64,
    used_space_proportion: f64,
}
```

### 硬盘信息demo ### 

返回数据案例：

```text
[
 DisksInfo { name: "枫叶", drive_letter: "F:\\", total_space: 1024191361024, available_space: 379198550016, used_space_proportion: 62.98 },
 DisksInfo { name: "系统", drive_letter: "C:\\", total_space: 214748696576, available_space: 34859315200, used_space_proportion: 83.77 }, 
 DisksInfo { name: "文档", drive_letter: "D:\\", total_space: 216582320128, available_space: 53451968512, used_space_proportion: 75.32 }, 
 DisksInfo { name: "软件", drive_letter: "E:\\", total_space: 522596642816, available_space: 181838790656, used_space_proportion: 65.2 }
]
```

### 实例化 System ###

实例化 System 后我们需要刷新一下数据，才能获取到最新的信息
然后声明一个空的 `Vec<DisksInfo>` 来存储数据，并返回。

```rust
use sysinfo::{Disks, Networks, System};
pub fn get_disks_info() -> Vec<DisksInfo> {
    // 实例化
    let mut sys = System::new();
    // 更新数据
    sys.refresh_all();

    let mut disks_list: Vec<DisksInfo> = Vec::new();

    disks_list
}
```

### 获取数据

通过 `Disks::new_with_refreshed_list();`获取到所有硬盘信息。

```rust
use sysinfo::{Disks, Networks, System};
pub fn get_disks_info() -> Vec<DisksInfo> {
    // 实例化
    let mut sys = System::new();
    // 更新数据
    sys.refresh_all();

    let mut disks_list: Vec<DisksInfo> = Vec::new();

    let disks = Disks::new_with_refreshed_list();
    for disk in disks.list() {
        // 提取磁盘名称、驱动器字母、总空间和可用空间
         // 获取磁盘名称
        let name = os_str_to_string(disk.name()); 
        // 获取驱动器字母
        let drive_letter = path_to_string(disk.mount_point());  
        // 获取总空间
        let total_space = disk.total_space();  
        // 获取可用空间
        let available_space = disk.available_space();  

        // 创建一个 `DisksInfo` 结构体
        let disk_info = DisksInfo {
            name,
            drive_letter,
            total_space,
            available_space,
            used_space_proportion: ((1f64 - (available_space as f64 / total_space as f64)) * 10000f64).round() / 100f64,
        };
        // 将结构体添加到列表中
        disks_list.push(disk_info);
    }

    disks_list
}
```

**工具方法**

```rust
fn os_str_to_string(os_str: &std::ffi::OsStr) -> String {
    let str = os_str.to_str();
    match str {
        Some(s) => s.to_string(),
        None => "".to_string(),
    }
}

fn path_to_string(path: &std::path::Path) -> String {
    let str = path.to_str();
    match str {
        Some(s) => s.to_string(),
        None => "".to_string(),
    }
}

```

## 获取cpu占用率等信息

因为我们需要 每隔一段时间，就要获取一次信息。如果我们直接在主进程中获取，就会阻塞主线程，导致程序无法继续执行。
所以我们需要使用新的线程来获取信息。

### 相关依赖

```rust
use tokio::sync::Mutex;
```

### 创建异步函数

`state: String` 一个状态标识符（如 "start" 或 "stop"），用于控制是否启动或停止获取系统资源的线程。

```rust
pub async fn get_system_usage(state: String) {
    match state.as_str() {
        "start" => {
            
        },
        "stop" => {
            
        },
        _ => {
            println!("参数错误");
        }
    }
}
```

### 创建一个静态变量

* **THREAD_CONTROL** 是一个惰性初始化的静态变量。它用于控制系统资源获取线程的启动和停止。
* **LazyLock** 用于保证在多线程环境下对 **THREAD_CONTROL** 进行惰性初始化，确保在首次使用时才会初始化。
* **Mutex<(bool, Option<tokio::task::JoinHandle<()>>)>** 是一个 **Mutex**，内部存储一个元组 (bool, Option<tokio::task::
  JoinHandle<()>>)，表示一个布尔值（是否正在运行）和一个 tokio::task::JoinHandle（用于控制线程的句柄）。

```rust
static THREAD_CONTROL: LazyLock<Mutex<(bool, Option<tokio::task::JoinHandle<()>>)>> = LazyLock::new(|| Mutex::new((false, None)));
```

**THREAD_CONTROL** 使用 **LazyLock** 和 **Mutex** 来确保对共享状态的访问是线程安全的，同时惰性初始化使得只有在需要时才会创建锁。

### 启动（创建）或停止线程

首先声明一个 control：使用 THREAD_CONTROL.lock().await 获取对静态变量的可变访问。

```rust
let mut control = THREAD_CONTROL.lock().await;
```

然后检查当前线程是否已经在运行：

```rust
if !control.0 {
    *control = (true, Some(tokio::spawn(async move {
        // 获取系统信息的代码
    })));
}
```

*control 代表对 control 变量中存储的值进行解引用并修改
如果线程尚未启动（control.0 为 false），就通过 tokio::spawn 启动一个异步任务，这个任务将会在后台循环执行，用于获取系统资源数据。
tokio::spawn 用于在独立线程上执行异步任务，async move 表示这是一个闭包，且会移走其捕获的变量，确保线程在运行时可以自由访问这些变量。

```rust
pub async fn get_system_usage(state: String) {
    let mut control = THREAD_CONTROL.lock().await;
    match state.as_str() {
        "start" => {
            if !control.0 {
                *control = (true, Some(tokio::spawn(async move {
                    
                })));
            }
        },
        "stop" => {
            if control.0 {
                *control = (false, control.1.take());
            }
        },
        _ => {
            println!("参数错误");
        }
    }
}
```

在停止线程时，我们首先检查线程是否正在运行。如果正在运行，则将控制标志设置为 false，并移除线程句柄。

### 获取系统信息

后面获取系统信息就简单了

1. **CPU 使用率：** 通过 sys.global_cpu_usage() 获取。
2. **内存使用率：** 通过 sys.used_memory() 和 sys.total_memory() 获取。
3. **网络下载速度：** 通过网络接口的数据包接收总量来计算下载速度。

```rust
// 上次获取的 当前接口接收的总数据量
let mut previous_received: u64 = 0;
// 上次获取的时间戳
let mut previous_timestamp = tokio::time::Instant::now();
// 初始化下载速度为0
let mut download_speed: f64 = 0.0;

loop {
    let control = THREAD_CONTROL.lock().await;
    if !control.0 {
        break;
    }
    // 刷新系统信息
    sys.refresh_all();
    // 获取cpu 使用率
    let cpu_usage = sys.global_cpu_usage();
    // 获取内存使用率
    let memory_usage = (sys.used_memory() as f64 / sys.total_memory() as f64) * 100.0;
    // 创建并刷新网络接口列表
    let networks = Networks::new_with_refreshed_list();
    // 遍历网络接口及其数据
    for (interface_name, data) in &networks {
        // 获取当前接口接收的总数据量
        let total_received = data.total_received();
        // 获取当前接口传输的总数据量
        let total_transmitted = data.total_transmitted();

        // 计算下载速度
        let elapsed_time = previous_timestamp.elapsed();
        if elapsed_time.as_secs() > 0 {
            // 计算自上次调用以来接收的数据量
            let data_received_since_last_call = total_received - previous_received;
            // 计算下载速度（单位：字节/秒）
            download_speed = data_received_since_last_call as f64 / elapsed_time.as_secs_f64();
       
        }

        // 更新前一个接收数据量和时间戳
        previous_received = total_received;
        previous_timestamp = tokio::time::Instant::now();
    }
    
    // 设置 loop 的休眠时间
    tokio::time::sleep(std::time::Duration::from_secs(2)).await;
}

```
在这段代码中，使用 let control = THREAD_CONTROL.lock().await; 在 loop 中重新获取 control 锁，而不是直接使用外面的 control，是因为 THREAD_CONTROL 是一个异步的锁（即 Mutex），它需要在每次访问时重新获取锁，并且每次访问都可能是异步的，因此不能直接在循环中反复使用同一个 control 变量。

### 完整代码

```rust
use std::sync::{ LazyLock};
use sysinfo::{Disks, Networks, System};
use tokio::sync::Mutex;

static THREAD_CONTROL: LazyLock<Mutex<(bool, Option<tokio::task::JoinHandle<()>>)>> = LazyLock::new(|| Mutex::new((false, None)));

#[command]
pub async fn get_system_usage(app: AppHandle, state:String) {
    let main_window = app.get_window("main").unwrap();

    let mut control = THREAD_CONTROL.lock().await;

    match state.as_str() {
        "start" => {
            if !control.0 {
                *control = (true, Some(tokio::spawn(async move {
                    let mut sys = System::new();

                    let mut previous_received: u64 = 0;
                    let mut previous_timestamp = tokio::time::Instant::now();
                    let mut download_speed: f64 = 0.0;

                    loop {
                        {
                            let control = THREAD_CONTROL.lock().await;
                            if !control.0 {
                                break;
                            }
                        }
                        // 刷新系统信息
                        sys.refresh_all();

                        // 获取cpu 占用率
                        let cpu_usage = sys.global_cpu_usage();

                        // 获取内存占用率
                        let memory_usage = (sys.used_memory() as f64 / sys.total_memory() as f64) * 100.0;

                        // 创建并刷新网络接口列表
                        let networks = Networks::new_with_refreshed_list();

                        // 遍历网络接口及其数据
                        for (interface_name, data) in &networks {

                            let total_received = data.total_received();
                            let total_transmitted = data.total_transmitted();

                            // 计算下载速度
                            let elapsed_time = previous_timestamp.elapsed();
                            if elapsed_time.as_secs() > 0 {
                                let data_received_since_last_call = total_received - previous_received;
                                download_speed = data_received_since_last_call as f64 / elapsed_time.as_secs_f64();

                            }

                            // 更新前一个接收数据量和时间戳
                            previous_received = total_received;
                            previous_timestamp = tokio::time::Instant::now();
                        }
                        
                        tokio::time::sleep(std::time::Duration::from_secs(2)).await;
                    }
                })));
            }
        },
        "stop" => {
            if control.0 {
                *control = (false, control.1.take());
            }
        },
        _ => {
            println!("参数错误");
        }
    }
}

```

