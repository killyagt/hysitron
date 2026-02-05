import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TriboScannerSimulator {
    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(10005);
        System.out.println("仪器模拟器已启动，等待中间层连接...");

        Socket client = server.accept(); //阻塞
        System.out.println("主机已连接！");

        DataOutputStream out = new DataOutputStream(client.getOutputStream());
        DataInputStream in = new DataInputStream(client.getInputStream());

        // 模拟步骤 1: 按照手册第14页，连接后主动发送 "Ready to Load Sample" (ID: 1)
        TriboMessage ready = new TriboMessage(1, "Triboscan in Loading Position");
        out.write(ready.toBytes());
        System.out.println("已发送: TS_READYTOLOADSAMPLE");


        int busyCount = 0;// 计数器，用来模拟测试需要时间

        byte[] header = new byte[12];
        while (in.read(header) != -1) {
            ByteBuffer bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            bb.getInt(); // rw
            int type = bb.getInt();
            int len = bb.getInt();

            byte[] msgBody = new byte[len];
            in.readFully(msgBody);
            String content = new String(msgBody);
            System.out.println("仪器收到指令 ID: " + type);

            if (type == 2) { // 收到：样品已装载
                out.write(new TriboMessage(23, "Quick approach started").toBytes());
            }
            else if (type == 5) { // 收到：开始方法任务
                System.out.println("仪器：开始执行测试任务...");
                busyCount = 0; // 每个新点开始 进度归零
                // 此时不回信，现实中机械臂开始操作，等主机来问状态
            }
            else if (type == 11) { // 收到：HOST_REQ_STATUS (状态查询)
                if (busyCount < 3) {
                    // 模拟前3次查询都返回：我很忙 (ID 4: TS_BUSY)
                    out.write(new TriboMessage(4, "TS_BUSY: Indenting...").toBytes());
                    busyCount++;
                    System.out.println("仪器：正在工作 (" + busyCount + "/3)");
                } else {
                    // 第4次查询返回：全部完成 (ID 27: HOST_OPERATIONCOMPLETED)
                    createMockResultFile("Batch_Point");
                    out.write(new TriboMessage(27, "All operations completed").toBytes());
                    System.out.println("仪器：报告任务已完成！");
                    busyCount = 0; // 重置计数器
                }
            }
            else if (type == 10) { // 收到：HOST_XYCOORDINATES
                System.out.println("仪器：收到移动指令，目标坐标为 -> " + content);

                // 模拟手册第 27 页流程：先回复“正在移动 (ID 22)”
                // 注意：手册中 ID 22 也用于汇报作业状态，这里模拟移动状态
                out.write(new TriboMessage(22, "TS_JOB_EXEC_STATUS: Moving Stages...").toBytes());

                // 模拟移动需要一点时间
                Thread.sleep(1000);
                System.out.println("仪器：移动已到位。");
            }
        }
    }


    // 模拟生成实验数据文件
    public static void createMockResultFile(String testName) {
        try {
            // 文件名模拟手册格式：TestName_Time.txt
            File file = new File("Result_" + testName + ".txt");
            FileWriter writer = new FileWriter(file);

            // 写入模拟数据：硬度(H) 和 模量(Er)
            // 模拟一个随机硬度值 10.0 ~ 12.0
            double hardness = 10.0 + Math.random() * 2.0;
            double modulus = 150.0 + Math.random() * 10.0;

            writer.write("TestName: " + testName + "\n");
            writer.write("Hardness: " + String.format("%.2f", hardness) + " GPa\n");
            writer.write("Modulus: " + String.format("%.2f", modulus) + " GPa\n");
            writer.close();

            System.out.println("（模拟器动作）已生成数据文件: " + file.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}