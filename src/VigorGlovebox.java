import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;


//手套箱驱动
public class VigorGlovebox {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    // 根据手册 B 部分：服务器 IP 192.168.7.91，Modbus TCP 默认端口 502
    public void connect(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        System.out.println(">>> [手套箱] 成功连接到环境监控系统");
    }


    /**
     * 读取环境数据 (Modbus TCP 简易实现)
     * 手册地址：40001 (压), 40003 (氧), 40005 (水), 40007 (温)
     */
    public GloveboxData readEnvironment() throws IOException {
        // 构造 Modbus TCP 请求报文 (读取寄存器 0-8，共4个FLOAT)
        // 这里的二进制字节是 Modbus TCP 的标准格式
        byte[] request = {
                0, 1,           // Transaction ID
                0, 0,           // Protocol ID (0 = Modbus)
                0, 6,           // Length (后面还有6字节)
                1,              // Unit ID (从站号)
                3,              // Function Code (03 = Read Holding Registers)
                0, 0,           // Starting Address (40001 对应内部地址 0)
                0, 8            // Quantity (读取8个寄存器，因为1个FLOAT占2个)
        };

        out.write(request);
        out.flush();

        // 接收响应
        byte[] response = new byte[30]; // 预留足够空间
        in.read(response);

        // 解析 FLOAT 数据 (从第 9 字节开始是数据区)
        ByteBuffer buffer = ByteBuffer.wrap(response, 9, 16);
        buffer.order(ByteOrder.BIG_ENDIAN); // PLC 通常是大端，如果数据不对就换 LITTLE_ENDIAN

        GloveboxData data = new GloveboxData();
        data.pressure = buffer.getFloat();    // 读取第一个 FLOAT
        data.oxygen = buffer.getFloat();      // 读取第二个 FLOAT
        data.water = buffer.getFloat();       // 读取第三个 FLOAT
        data.temperature = buffer.getFloat(); // 读取第四个 FLOAT

        return data;
    }

    public void disconnect() throws IOException {
        if (socket != null) socket.close();
    }
}