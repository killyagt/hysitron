import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("========== 实验自动化系统控制台 ==========");
        System.out.println("1. 启动 [压痕仪] 模拟器 (Simulator)");
        System.out.println("2. 启动自动化中间层 (Host)");
        System.out.println("3. 测试 [手套箱] 驱动连接 (Glovebox Test)");
        System.out.println("4. 启动 [手套箱] 模拟器 (Glovebox Simulator)"); // 新增
        System.out.println("==========================================");
        System.out.print("请选择启动模式：");

        int choice = scanner.nextInt();

        try {
            if (choice == 1) {
                // 启动压痕仪模拟器 (Port 10005)
                TriboScannerSimulator.main(null);

            } else if (choice == 2) {
                // 启动指挥官逻辑
                AutomationHost host = new AutomationHost();
                System.out.println(">>> 正在初始化多机联动系统...");
                // 调试开发建议 IP 都填 127.0.0.1
                host.connectAll("127.0.0.1", "127.0.0.1");
                host.startWorkflow();

            } else if (choice == 3) {
                // 单独测试手套箱读取功能
                System.out.println(">>> 正在执行手套箱单机连通性测试...");
                VigorGlovebox gb = new VigorGlovebox();
                try {
                    // 连本地模拟器，确保端口号与模拟器一致（建议 5020）
                    gb.connect("127.0.0.1", 5020);
                    GloveboxData data = gb.readEnvironment();
                    System.out.println("成功读回数据 -> " + data.toString());
                    gb.disconnect();
                } catch (Exception e) {
                    System.out.println("!!! 连接失败：" + e.getMessage());
                }

            } else if (choice == 4) {
                // 启动手套箱模拟器 (Port 5020)
                // [!] 逻辑标注：这里直接调用模拟器文件的 main 方法
                VigorGloveboxSimulator.main(null);
            }

        } catch (Exception e) {
            System.err.println("程序运行发生严重错误：");
            e.printStackTrace();
        }
    }
}