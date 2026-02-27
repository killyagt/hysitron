import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 模拟威格手套箱 PLC (Modbus TCP)
 */
public class VigorGloveboxSimulator {
    public static void main(String[] args) throws Exception {
        ServerSocket server = new ServerSocket(502); // Modbus 默认端口
        System.out.println("手套箱 PLC 模拟器已启动，等待连接 (Port: 502)...");

        while (true) {
            try (Socket client = server.accept();
                 DataInputStream in = new DataInputStream(client.getInputStream());
                 DataOutputStream out = new DataOutputStream(client.getOutputStream())) {

                System.out.println("主机已连入手套箱模拟器");

                byte[] request = new byte[12];
                while (in.read(request) != -1) {
                    // 解析请求（简单模拟，不校验地址）
                    // 模拟生成 FLOAT 数据
                    // 以下数值为模拟业务场景，非手册固定要求
                    float oxygen = 0.05f + (float)Math.random() * 0.1f; // 模拟氧气在 0.05-0.15 波动
                    float water = 0.1f;
                    float pressure = 2.5f;
                    float temp = 24.5f;

                    // 构造 Modbus TCP 响应包
                    // 头部(7字节) + 字节数(1字节) + 数据(16字节)
                    ByteBuffer resp = ByteBuffer.allocate(25);
                    resp.order(ByteOrder.BIG_ENDIAN);

                    resp.putShort((short) 1);    // Transaction ID
                    resp.putShort((short) 0);    // Protocol ID
                    resp.putShort((short) 19);   // Length (后面19字节)
                    resp.put((byte) 1);          // Unit ID
                    resp.put((byte) 3);          // Function Code
                    resp.put((byte) 16);         // Byte Count (4个FLOAT=16字节)

                    resp.putFloat(pressure);
                    resp.putFloat(oxygen);
                    resp.putFloat(water);
                    resp.putFloat(temp);

                    out.write(resp.array());
                    out.flush();
                }
            } catch (Exception e) {
                System.out.println("模拟器连接断开");
            }
        }
    }
}