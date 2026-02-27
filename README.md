# 实验室自动化控制中间层 (Lab Automation Middleware)

本项目旨在为 **Hysitron TI 990 纳米压痕仪** 与 **Vigor 威格手套箱** 构建一个统一的自动化控制中间层。通过 Java 实现多设备联动、实时环境监测、安全准入判定以及实验数据的全维度自动汇总。

## 核心功能 (Features)

1.  **多协议设备驱动 (Multi-protocol Support)**
    *   **Hysitron Indenter**: 基于厂家私有二进制协议（12字节包头 + 字符串指令）。
    *   **Vigor Glovebox**: 基于工业标准 **Modbus TCP** 协议。
2.  **安全准入机制 (Safety Interlock)**
    *   **Z轴保护**: 在执行任何横向移动前，强制执行 Z 轴归位（Homing）并进行轮询验证。
    *   **环境联动**: 实时监测手套箱氧气/水分浓度。若环境超标（默认阈值 0.5ppm），自动挂起实验任务，待环境达标后恢复。
3.  **批量测试与数据闭环 (Data Convergence)**
    *   **清单导入**: 支持通过 `test_points.csv` 批量加载测试坐标与实验方法。
    *   **自动汇总**: 实验结束后自动生成 `Final_Report.csv`，将机械力学性能（硬度、模量）与实验瞬时的环境参数（O2, H2O）进行全维度合流记录。
4.  **数字孪生仿真 (Simulation)**
    *   内置压痕仪与手套箱双设备模拟器，支持在脱离硬件环境的情况下进行完整的自动化逻辑演练。

## 技术架构 (Architecture)

采用**“驱动-逻辑-数据”**三层解耦架构，具备良好的扩展性：

-   **Driver层** (`HysitronIndenter`, `VigorGlovebox`): 封装底层二进制打包、Socket 通信及协议解析逻辑。
-   **Logic层** (`AutomationHost`): 负责核心实验流程调度、安全判定及设备间的联动指令。
-   **Model层** (`TestTask`, `TestResult`, `GloveboxData`): 统一定义实验任务与结果的数据结构。

## 目录结构 (Project Structure)

```text
src/
├── Main.java                    # 程序入口，支持切换模拟器/主机模式
├── AutomationHost.java          # 自动化指挥官（核心业务逻辑）
├── HysitronIndenter.java        # 压痕仪驱动（私有二进制协议）
├── VigorGlovebox.java           # 手套箱驱动（Modbus TCP协议）
├── TriboMessage.java            # 通信协议格式化工具（处理字节序）
├── TestTask.java                # 任务模型（坐标 + 方法名）
├── TestResult.java              # 结果模型（力学数据 + 环境数据）
├── GloveboxData.java            # 环境数据模型
├── TriboScannerSimulator.java   # 压痕仪仿真模拟器
└── VigorGloveboxSimulator.java  # 手套箱 PLC 仿真模拟器
