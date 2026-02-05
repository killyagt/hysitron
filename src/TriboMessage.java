import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class TriboMessage {
    public int readWrite; // 1=Write, 2=Read
    public int type;      // 消息 ID (手册第10页)
    public int length;    // 字符串长度
    public String message;// 消息内容字符串

    public TriboMessage(int type, String message) {
        this.readWrite = 1; // 主机发送默认为 WRITE
        this.type = type;
        this.message = message;
        this.length = message.getBytes(StandardCharsets.UTF_8).length;
    }

    // 将 Java 对象转为二进制字节数组（用于发送）
    public byte[] toBytes() {
        byte[] msgBytes = message.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(12 + msgBytes.length);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // 必须是小端序
        buffer.putInt(readWrite);
        buffer.putInt(type);
        buffer.putInt(length);
        buffer.put(msgBytes);
        return buffer.array();
    }

    // 从二进制字节流解析消息（用于接收）
    public static TriboMessage fromBytes(ByteBuffer buffer) {
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int rw = buffer.getInt();// 前4个字节 读还是写
        int type = buffer.getInt();// 5到8个字节 命令ID是多少
        int len = buffer.getInt();// 6到12个字节 文字字数
        byte[] msgBytes = new byte[len];// 消息内容
        buffer.get(msgBytes);
        String msg = new String(msgBytes, StandardCharsets.UTF_8);

        TriboMessage tm = new TriboMessage(type, msg);
        tm.readWrite = rw;
        return tm;
    }
}