//手套箱数据模型
public class GloveboxData {
    public float pressure;    // 箱压 (mbar)
    public float oxygen;      // 含氧率 (ppm)
    public float water;       // 含水率 (ppm)
    public float temperature; // 箱内温度 (℃)

    @Override
    public String toString() {
        return String.format("环境数据: 压力=%.2f mbar, 氧气=%.2f ppm, 水分=%.2f ppm, 温度=%.1f ℃",
                pressure, oxygen, water, temperature);
    }
}