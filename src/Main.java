import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请选择启动模式：");
        System.out.println("1. 启动仪器模拟器 (Simulator)");
        System.out.println("2. 启动自动化中间层 (Host)");

        int choice = scanner.nextInt();

        try {
            if (choice == 1) {
                // 启动模拟器逻辑 静态调用
                TriboScannerSimulator.main(null);
            } else if (choice == 2) {
                // 启动控制逻辑 实例调用
                AutomationHost host = new AutomationHost();
                host.connect("127.0.0.1", 10005);
                host.startWorkflow();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}