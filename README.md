# Hysitron TI 990 TCP 自动化控制中间层 


## 1. 核心功能
*   **安全归位 (Safety Homing)**：在执行任何机械移动前，自动轮询 Z 轴状态，确保探针处于安全高度，防止撞针。
*   **批量任务加载 (Batch Processing)**：支持通过 `test_points.csv` 文件导入坐标清单及指定的测试方法。
*   **实时监控 (Status Polling)**：基于 TCP 协议实现对仪器状态（Busy/Error/Completed）监控。
*   **数据自动汇总 (Data Collection)**：自动解析仪器生成的原始数据文件，并汇总导出为 `Final_Report.csv` 报表。

## 2. 项目结构说明
*   `src/AutomationHost.java`: 指挥官程序，负责核心自动化逻辑与 TCP 通信。
*   `src/TriboScannerSimulator.java`: 仪器模拟器，完整模拟 TI 990 的协议反馈，用于脱机开发调试。
*   `src/TriboMessage.java`: 协议转换工具，处理符合手册标准的小端序（Little-Endian）二进制打包。
*   `test_points.csv`: 任务输入模板（格式：X, Y, MethodName）。

## 3. 运行指南
本程序支持脱机演示，无需连接真实仪器硬件：
1.  **启动模拟器**：运行 `Main.java` 并选择模式 `1`。
2.  **启动中间层**：运行 `Main.java` 并选择模式 `2`。
3.  **查看结果**：程序运行结束后，可在项目根目录下查看生成的 `Final_Report.csv`。

## 4. 开发环境
*   语言：Java 21 (JDK 21)
*   协议：TCP/IP (Socket)
