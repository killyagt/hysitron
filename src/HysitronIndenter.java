import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

//压痕仪驱动类
public class HysitronIndenter {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public void connect(String ip, int port) throws IOException {
        socket = new Socket(ip, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        System.out.println(">>> [压痕仪] 连接成功 (" + ip + ":" + port + ")");
    }

    // 将原来的 sendMessage 挪到这里，并改名为 send
    public void send(int type, String content) throws IOException {
        TriboMessage msg = new TriboMessage(type, content);
        out.write(msg.toBytes());
        out.flush();
    }

    // 将原来的 receiveMessage 挪到这里，并改名为 receive
    public TriboMessage receive() throws IOException {
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

    // --- 具体机器指令 ---

    public void moveXY(double x, double y) throws IOException {
        String coords = String.format("%.4f:%.4f", x, y);
        send(10, coords);
        System.out.println(">>> 主机：下令移动到坐标  [" + coords + "]");
    }

    /**
     * 发送 Z 轴归位指令并轮询直到完成
     * 参考手册第 16 页 (Status Request Handling) 和 第 21 页 (HOMEZAXIS)
     */
    public void homeZAxis() throws IOException, InterruptedException {
        System.out.println(">>> [安全检查] 正在启动 Z 轴归位...");
        send(3, "Home Z Axis");// 发送归位指令 ID 3

        boolean isHoming = true;
        while (isHoming) {
            // 1. 稍等再问
            Thread.sleep(1000);

            // 2. 发送状态查询 (ID 11)
            send(11, "Query Homing Status");

            // 3. 根据返回的消息 ID 判断状态
            TriboMessage status = receive();
            // 根据手册第 18/21 页，HOMEZAXIS 完成后会回到 INITIALSTATE，发送 ID 1

            if (status.type == 1) {
                // 根据手册第 18/21 页，HOMEZAXIS 完成后会回到 INITIALSTATE，发送 ID 1
                System.out.println(">>> [安全检查] 接收到 ID 1: Z 轴已成功归位。");
                isHoming = false; // 退出循环
            }
            else if (status.type == 4 || status.type == 22) {
                // ID 4 (Busy) 或 ID 22 (Job Status) 表示还在动
                System.out.println(">>> [安全检查] 仪器忙碌中: " + status.message);
            }
            else if (status.type == 12) {
                // ID 12 (Error)
                System.out.println("!!! [紧急停止] 归位过程中发生错误！");
                throw new RuntimeException("Z-Axis Homing Failed");
            }
        }
        System.out.println(">>> [安全检查] 归位验证通过，载物台移动现在安全的。");
    }

    public void disconnect() throws IOException {
        if (socket != null) socket.close();
    }
}