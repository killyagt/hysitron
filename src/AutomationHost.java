// 文件位置：src/AutomationHost.java
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AutomationHost {
    // 持有具体的设备对象
    private HysitronIndenter indenter; // 压痕仪驱动
    private VigorGlovebox glovebox;     // 手套箱驱动
    private List<TestResult> summaryData = new ArrayList<>();

    // 连接所有设备
    public void connectAll(String indenterIp, String gbIp) throws IOException {
        indenter = new HysitronIndenter();
        indenter.connect(indenterIp, 10005);

        glovebox = new VigorGlovebox();
        glovebox.connect(gbIp, 502);

        System.out.println(">>> [系统] 所有实验设备已就绪");
    }

    public void startWorkflow() throws IOException, InterruptedException {
        // 1. 压痕仪初始握手
        TriboMessage firstMsg = indenter.receive();
        if (firstMsg.type != 1) {
            System.out.println("错误：压痕仪未就绪");
            return;
        }

        // 2. 安全归位
        indenter.homeZAxis();

        // 3. 加载清单
        List<TestTask> taskList = loadPointsFromFile("test_points.csv");
        summaryData.clear();

        // 4. 批量循环
        for (int i = 0; i < taskList.size(); i++) {
            TestTask task = taskList.get(i);

            System.out.println("\n===== 正在处理点位 " + (i + 1) + " =====");

            // --- 先检查手套箱环境 ---
            // !!! 0.5ppm阈值为自己设定的值，手册未强制规定 !!!
            GloveboxData env = glovebox.readEnvironment();
            while (env.oxygen > 0.5f) {
                System.out.println("!!! [环境警告] 氧气浓度 " + env.oxygen + " ppm 过高，暂停实验...");
                Thread.sleep(10000);
                env = glovebox.readEnvironment();
            }

            // --- 实验点：操作压痕仪 ---
            indenter.moveXY(task.x, task.y);
            indenter.receive(); // 读掉 ID 22 移动回执

            indenter.send(2, "Sample In Position");
            indenter.receive(); // 读掉 ID 23 靠近回执

            indenter.send(5, task.methodName);

            // 监控进度
            boolean isPointRunning = true;
            while (isPointRunning) {
                Thread.sleep(2000);//设定两秒询问一次

                indenter.send(11, "Query Status");
                TriboMessage status = indenter.receive();

                if (status.type == 4) {
                    // ID 4: TS_BUSY
                    System.out.println("状态报告：仪器正在忙碌 [" + status.message + "]");
                }
                if (status.type == 27) {
                    // 记录结果 (带上当时的环境氧气、水分数据)
                    readAndRecordResult("Result_Batch_Point.txt", task, env);
                    isPointRunning = false;
                }
                else if (status.type == 12) {
                    System.out.println("仪器报错，终止任务");
                    return;
                }
            }
        }

        indenter.send(27, "All Jobs Done");
        System.out.println("\n>>> [任务汇总] 所有批量测试点已执行完毕。安全退出。");
        saveSummaryToCSV();
    }

    /**
     * 解析结果文件并整合环境数据存入汇总列表
     * @param filename 压痕仪生成的结果文件名
     * @param task 当前执行的测试任务(包含坐标)
     * @param env 当前手套箱的环境数据
     */
    private void readAndRecordResult(String filename, TestTask task, GloveboxData env) {
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println(">>> [警告] 找不到结果文件: " + filename + "，跳过数据记录。");
            return;
        }

        // 使用 try-with-resources 自动关闭文件流 (Java 7+ 推荐写法)
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            // 临时存储解析出的力学性能
            double hardness = 0.0;
            double modulus = 0.0;
            String actualTestName = task.methodName; // 默认使用任务中的名字

            while ((line = reader.readLine()) != null) {
                // 1. 解析测试名称
                if (line.startsWith("TestName:")) {
                    actualTestName = line.split(":")[1].trim();
                }
                // 2. 解析硬度值 (Hardness)
                else if (line.startsWith("Hardness:")) {
                    // 逻辑拆解：去掉 "Hardness:", 去掉 "GPa", 剩下的转数字
                    String val = line.split(":")[1].replace("GPa", "").trim();
                    hardness = Double.parseDouble(val);
                }
                // 3. 解析模量值 (Modulus)
                else if (line.startsWith("Modulus:")) {
                    String val = line.split(":")[1].replace("GPa", "").trim();
                    modulus = Double.parseDouble(val);
                }
            }

            // [!] 核心联动逻辑：
            // 创建一个包含【坐标】+【力学数据】+【实验时的氧/水浓度】的完整结果对象
            TestResult finalResult = new TestResult(
                    actualTestName,
                    task.x,
                    task.y,
                    hardness,
                    modulus,
                    env.oxygen, // 手套箱实时氧气
                    env.water   // 手套箱实时水分
            );

            // 将整合后的结果加入“记事本”
            summaryData.add(finalResult);

            System.out.println(">>> [数据闭环] 已成功记录点位数据：");
            System.out.println("    位置: [" + task.x + ", " + task.y + "]");
            System.out.println("    结果: H=" + hardness + " GPa, Er=" + modulus + " GPa");
            System.out.println("    环境: O2=" + env.oxygen + " ppm, H2O=" + env.water + " ppm");

        } catch (Exception e) {
            System.out.println("!!! [解析错误] 处理结果文件时发生异常: " + e.getMessage());
        } finally {
            // 解析完成后删除该临时结果文件，保持工作目录清洁
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public List<TestTask> loadPointsFromFile(String filename) {
        List<TestTask> tasks = new ArrayList<>(); // 改这里
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            System.out.println(">>> 正在读取任务清单: " + filename);

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split(",");

                // 现在我们需要至少 3 部分数据
                if (parts.length >= 3) {
                    double x = Double.parseDouble(parts[0].trim());
                    double y = Double.parseDouble(parts[1].trim());
                    String method = parts[2].trim(); // 读取第三列

                    tasks.add(new TestTask(x, y, method));
                    System.out.println("    任务加载: " + method + " @ [" + x + "," + y + "]");
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tasks;
    }


    public void saveSummaryToCSV() {
        try {
            String filename = "Final_Report.csv";
            PrintWriter writer = new PrintWriter(new FileWriter(filename));

            // 写表头
            writer.println("TestName,X,Y,Hardness(GPa),Modulus(GPa),Oxygen(ppm),Water(ppm)");

            // 遍历记事本，写每一行
            for (TestResult res : summaryData) {
                writer.println(res.toCSVString());
            }

            writer.close();
            System.out.println("\n========================================");
            System.out.println(" 汇总报告已生成: " + filename);
            System.out.println("========================================");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}