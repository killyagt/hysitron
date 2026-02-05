import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class AutomationHost {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private List<TestResult> summaryData = new ArrayList<>();

    public void connect(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        System.out.println("成功连接");
    }
    /**
     * 执行自动化批量测试工作流
     * 涵盖了手册中从 INITIALSTATE 到 ENDSTATE 的核心逻辑
     */
    public void startWorkflow() throws IOException, InterruptedException {
        // ---------------------------------------------------------
        // 1. 等待握手信号 (手册第14页: Initial Host Communication)
        // ---------------------------------------------------------
        TriboMessage firstMsg = receiveMessage();
        if (firstMsg.type != 1) {
            System.out.println("错误：未收到仪器的就绪信号！");
            return;
        }
        System.out.println(">>> 仪器已进入自动化模式，准备开始批量任务。");

        // ---------------------------------------------------------
        // 2. 定义批量坐标清单 (X, Y)
        // ---------------------------------------------------------
        List<double[]> pointList = loadPointsFromFile("test_points.csv");

        if (pointList.isEmpty()) {
            System.out.println("错误：未找到测试坐标，请检查 test_points.csv 文件！");
            return;
        }
        // ---------------------------------------------------------
        // 3. 核心批量循环 (针对清单里的每一个点)
        // ---------------------------------------------------------
        // 每次开始新任务前，清空记事本
        summaryData.clear();
        for (int i = 0; i < pointList.size(); i++) {
            double[] currentPoint = pointList.get(i);
            double targetX = currentPoint[0];
            double targetY = currentPoint[1];

            System.out.println("\n--------------------------------------------");
            System.out.println("正在执行任务 (" + (i + 1) + "/" + pointList.size() + "): 坐标 [" + targetX + ", " + targetY + "]");
            System.out.println("--------------------------------------------");

            // A. 移动载物台 (ID 10: HOST_XYCOORDINATES)
            moveXY(targetX, targetY);
            TriboMessage moveAck = receiveMessage();
            if (moveAck.type == 22) {
                System.out.println("动作：载物台正在移动中...");
            }
            // 额外休眠1秒，确保机械臂完全平稳
            Thread.sleep(1000);

            // B. 确认样品状态 (ID 2: HOST_SAMPLELOADED)
            // 告诉仪器：我已经把你挪到指定位置了，你可以准备压了
            sendMessage(2, "Sample In Position");
            receiveMessage(); // 接收 ID 23 (TS_SAMPLE_APPROACH_STATUS)
            System.out.println("动作：样品位置确认，开始快速靠近...");

            // C. 下达压痕测试任务 (ID 5: HOST_METHODID)
            String methodName = "Batch_Test_Point_" + (i + 1);
            sendMessage(5, methodName);
            System.out.println("动作：已下达压痕指令 [" + methodName + "]");

            // D. 状态监控循环 (ID 11: HOST_REQ_STATUS)
            // 对应手册第16页：直到收到 ID 27 (完成) 才会跳出
            boolean isPointRunning = true;
            while (isPointRunning) {
                Thread.sleep(2000); // 每2秒询问一次，避免占用过多CPU

                sendMessage(11, "Query Status");
                TriboMessage status = receiveMessage();

                if (status.type == 4) {
                    // ID 4: TS_BUSY
                    System.out.println("状态报告：仪器正在忙碌 [" + status.message + "]");
                }
                else if (status.type == 27) {
                    // ID 27: HOST_OPERATIONCOMPLETED
                    System.out.println("状态报告：当前点位任务已完成。");
                    readResultData("Result_Batch_Point.txt", targetX, targetY);
                    isPointRunning = false; // 结束内层循环，进入下一个点
                }
                else if (status.type == 12) {
                    // ID 12: TS_ERROR (手册第17页)
                    System.out.println("警告：仪器发生异常错误！正在停止自动化流程...");
                    return;
                }
            }
        }

        // ---------------------------------------------------------
        // 4. 结束流程 (手册第10页: HOST_OPERATIONCOMPLETED)
        // ---------------------------------------------------------
        sendMessage(27, "All Batch Jobs Completed");
        System.out.println("\n>>> [任务汇总] 所有批量测试点已执行完毕。安全退出。");
        saveSummaryToCSV();
    }


    // 此方法负责把两个 double 数字变成手册要求的字符串格式，并发送 ID 10
    public void moveXY(double x, double y) throws IOException {
        // 手册第34页规定格式为 X:Y，保留4位小数
        String coords = String.format("%.4f:%.4f", x, y);
        sendMessage(10, coords); // ID 10: HOST_XYCOORDINATES
        System.out.println(">>> 主机：下令移动到坐标 [" + coords + "]");
    }

    // 此方法读取 CSV 文件并解析为坐标列表
    public List<double[]> loadPointsFromFile(String filename) {
        List<double[]> points = new ArrayList<>();
        try {
            // 建立文件读取流 (Head First Java 第14章)
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;

            System.out.println(">>> 正在读取文件: " + filename);
            while ((line = reader.readLine()) != null) {
                // 跳过空行
                if (line.trim().isEmpty()) continue;

                // 用逗号分割每一行： "10.0, 10.0" -> ["10.0", " 10.0"]
                String[] parts = line.split(",");

                if (parts.length >= 2) {
                    double x = Double.parseDouble(parts[0].trim());
                    double y = Double.parseDouble(parts[1].trim());
                    points.add(new double[]{x, y});
                    System.out.println("    已加载坐标: " + x + ", " + y);
                }
            }
            reader.close();
        } catch (Exception e) {
            System.out.println("！！读取文件出错: " + e.getMessage());
        }
        return points;
    }


    // 读取最新的结果文件
    // 修改后的读取方法：接收坐标信息，以便一起存下来
    public void readResultData(String filename, double x, double y) {
        File file = new File(filename);
        if (!file.exists()) return;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;

            // 临时变量，用来存从文件里读到的值
            String name = "Unknown";
            double h = 0.0;
            double m = 0.0;

            while ((line = reader.readLine()) != null) {
                // 解析逻辑：根据模拟器生成的文件格式来拆解
                // 文件样例：
                // TestName: Batch_Point
                // Hardness: 11.23 GPa

                if (line.startsWith("TestName:")) {
                    name = line.split(":")[1].trim();
                }
                else if (line.startsWith("Hardness:")) {
                    // 取冒号后面的部分，再把 "GPa" 删掉，最后转成数字
                    String val = line.split(":")[1].replace("GPa", "").trim();
                    h = Double.parseDouble(val);
                }
                else if (line.startsWith("Modulus:")) {
                    String val = line.split(":")[1].replace("GPa", "").trim();
                    m = Double.parseDouble(val);
                }
            }
            reader.close();
            file.delete(); // 读完删除

            // 把解析出来的数据存进记事本 ---
            TestResult result = new TestResult(name, x, y, h, m);
            summaryData.add(result);
            System.out.println(">>> [数据已记录] " + name + " -> H=" + h);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    // 在所有任务结束后调用，生成总表
    public void saveSummaryToCSV() {
        try {
            String filename = "Final_Report.csv";
            PrintWriter writer = new PrintWriter(new FileWriter(filename));

            // 写表头
            writer.println("TestName,X,Y,Hardness(GPa),Modulus(GPa)");

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

    private void sendMessage(int type, String content) throws IOException {
        TriboMessage msg = new TriboMessage(type, content);
        out.write(msg.toBytes());
        out.flush();
    }

    private TriboMessage receiveMessage() throws IOException {
        byte[] header = new byte[12];
        in.readFully(header);
        ByteBuffer bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);

        int rw = bb.getInt();
        int type = bb.getInt();
        int len = bb.getInt();

        byte[] body = new byte[len];
        in.readFully(body);

        ByteBuffer fullBuffer = ByteBuffer.allocate(12 + len).order(ByteOrder.LITTLE_ENDIAN);
        fullBuffer.put(header);
        fullBuffer.put(body);
        fullBuffer.flip();

        return TriboMessage.fromBytes(fullBuffer);
    }

    public static void main(String[] args) {
        try {
            AutomationHost host = new AutomationHost();
            // 先启动模拟器，再运行此 main 函数连接 localhost
            host.connect("127.0.0.1", 10005);
            host.startWorkflow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}