前情提要：本文使用的tauri 版本为 2.0.0，若版本过旧，请自行修改依赖版本。

官网地址：[传送门](https://tauri.app/zh-cn/learn/system-tray/)

本文只涉及在 rust 中声明 系统托盘图标，若是需要使用 JavaScript 的， 请自行参考官方文档。

## 安装依赖项

```toml
[dependencies]
tauri = { version = "2", features = ["tray-icon"] }
```

## 创建 TrayIconBuilder 实例

### 创建菜单项

使用 `MenuItem::with_id(参数一，参数二，参数三，参数四)` 创建菜单项

1. 第一个参数是当前应用程序
2. 第二个参数是菜单项的id（唯一标识），
3. 第三个参数是菜单项展示的名称，
4. 第四个参数是是否启用，
5. 第四个参数是菜单项的快捷键（可选）

示例：

``` rust 
let open = MenuItem::with_id(app, "open", "打开主界面", true, Some("CmdOrControl+O"))?; 
let quit = MenuItem::with_id(app, "quit", "退出", true, None::<&str>)?;
```

### 配置菜单项

构建菜单，第一个参数是当前应用程序，第二个参数是菜单项列表

```rust
let menu = Menu::with_items(app, &[&open, &quit])?;
```

### 设置托盘图标

设置托盘图标的图标

自定义图标方法：

let icon_path = Path::new("src-tauri/icons/icon.png");

设置托盘图标的图标

.icon(tauri::Icon::File(icon_path.to_path_buf()))

```rust
let tray_menu = TrayIconBuilder::new()
    .icon(app.default_window_icon().unwrap().clone())
```

### 设置菜单 和 左键是否生效
```rust 
let tray_menu = TrayIconBuilder::new()
    .icon(app.default_window_icon().unwrap().clone())
    .menu(&menu)
    // 设置左键点击是否生效 默认情况下，菜单在左键和右键单击时都会显示。
    .menu_on_left_click(true)
```

### 处理托盘图标事件

处理托盘图标的事件

* click: 当光标收到一次左、右或中键单击时触发，包括有关是否释放鼠标按下的信息
* Double click: 当光标收到双击左、右或中键时触发
* Enter: 光标进入托盘图标区域时触发
* Move: 当光标在托盘图标区域周围移动时触发
* Leave: 当光标离开托盘图标区域时触发

```rust
let tray_menu = TrayIconBuilder::new()
    .icon(app.default_window_icon().unwrap().clone())
    .on_tray_icon_event(|tray, event| match event {
        
            // 增加 单击事件
            TrayIconEvent::Click {
                // 判断是否是左键
                button: MouseButton::Left,
                // 判断鼠标是否释放   
                button_state: MouseButtonState::Up, 
                ..
            } => {
                // 这里获取到 App 实例
                let app = tray.app_handle();   
                // 获取 main 窗口
                if let Some(window) = app.get_webview_window("main") {  
                    //  展示窗口
                    let _ = window.show(); 
                    // 窗口获取焦点   
                    let _ = window.set_focus();    
                }
            }
            _ => {}
    })
    // 构建系统托盘
    .build(app)?;
```

### 处理菜单事件
```rust
    // 处理托盘菜单的事件
    tray_menu.on_menu_event(|app, event| match event.id.as_ref() {
        // 打开主页面
        "open" => {
            // 这里你可以实现显示窗口的逻辑
            let window = app.get_window("main").unwrap();
            window.show().unwrap();
        }
        //  关闭主程序
        "quit" => {
            std::process::exit(0);
        }
        _ => {}
    });
```

### 完整代码
需要的基本设置写在了注释里：

```rust
/*
 * 获取系统托盘菜单
 */
pub fn get_system_tray_menu(app: &App) -> tauri::Result<TrayIcon> {
    // 创建菜单项
   // 第一个参数是当前应用程序
    // 第二个参数是菜单项的id，
    // 第三个参数是菜单项的名称，
    // 第四个参数是是否启用，
    // 第四个参数是菜单项的快捷键（可选） 
    // 注意：这里的快捷键只是展示作用，并不会监听快捷键是否触发。
    // Tauri 本身不直接提供全局快捷键的 API，但你可以通过调用第三方 Rust 库来实现全局快捷键监听;
    // 例如使用 tauri-plugin-global-shortcut 插件。
    let open = MenuItem::with_id(app, "open", "打开主界面", true, Some("CmdOrControl+O"))?;
    let quit = MenuItem::with_id(app, "quit", "退出", true, None::<&str>)?;
    
    // 构建菜单，第一个参数是当前应用程序，第二个参数是菜单项列表
    let menu = Menu::with_items(app, &[&open, &quit])?;

    // 构建托盘图标，设置图标、菜单、以及相关事件处理
    let tray_menu = TrayIconBuilder::new()
        // 设置托盘图标的图标
        // 自定义图标方法：
        // let icon_path = Path::new("src-tauri/icons/icon.png");
        // 设置托盘图标的图标
        // .icon(tauri::Icon::File(icon_path.to_path_buf()))
        .icon(app.default_window_icon().unwrap().clone())
        // 设置托盘图标的菜单
        .menu(&menu)
        // 设置左键点击是否生效 默认情况下，菜单在左键和右键单击时都会显示。
        .menu_on_left_click(true)
        // 处理托盘图标的事件
        // click: 当光标收到一次左、右或中键单击时触发，包括有关是否释放鼠标按下的信息
        // Double click: 当光标收到双击左、右或中键时触发
        // Enter: 光标进入托盘图标区域时触发
        // Move: 当光标在托盘图标区域周围移动时触发
        // Leave: 当光标离开托盘图标区域时触发
        .on_tray_icon_event(|tray, event| match event {
        
            // 增加 单击事件
            TrayIconEvent::Click {
                // 判断是否是左键
                button: MouseButton::Left,
                // 判断鼠标是否释放   
                button_state: MouseButtonState::Up, 
                ..
            } => {
                // 这里获取到 App 实例
                let app = tray.app_handle();   
                // 获取 main 窗口
                if let Some(window) = app.get_webview_window("main") {  
                    //  展示窗口
                    let _ = window.show(); 
                    // 窗口获取焦点   
                    let _ = window.set_focus();    
                }
            }
            _ => {}
        })
        // 构建系统托盘
        .build(app)?;

    // 处理托盘菜单的事件
    tray_menu.on_menu_event(|app, event| match event.id.as_ref() {
        // 打开主页面
        "open" => {
            // 这里你可以实现显示窗口的逻辑
            let window = app.get_window("main").unwrap();
            window.show().unwrap();
        }
        //  关闭主程序
        "quit" => {
            std::process::exit(0);
        }
        _ => {}
    });

    // 返回构建好系统托盘
    Ok(tray_menu)
}
```

## Tauri 应用构建器中使用

```rust
use crate::utils::system_tray_menu::get_system_tray_menu;
// 在 utils 的文件夹中创建 system_tray_menu.rs 文件 中 创建 get_system_tray_menu 函数
pub fn run() {
        tauri::Builder::default()
        .setup(|app| {

            // 调用 get_system_tray_menu 并处理可能的错误
            if let Err(err) = get_system_tray_menu(app) {
                println!("系统托盘创建失败: {}", err);
            }
            Ok(())
        })
        .run(tauri::generate_context!())
        .expect("error while running tauri application");
}
```

这样就处理好了。

作者水平有限，可能比较简陋，也或许有些错误，欢迎指正。
