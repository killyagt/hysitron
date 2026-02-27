// 这个类用来暂存单个测试点的数据
public class TestResult {
    public String testName; // 测试名称
    public double x;        // X坐标
    public double y;        // Y坐标
    public double hardness; // 硬度
    public double modulus;  // 模量
    public float o2;
    public float h2o;

    public TestResult(String name, double x, double y, double h, double m, float o2, float h2o) {
        this.testName = name;
        this.x = x;
        this.y = y;
        this.hardness = h;
        this.modulus = m;
        this.o2 = o2;
        this.h2o = h2o;
    }

    // 方便把这一行转成 CSV 格式的字符串
    public String toCSVString() {
        return String.format("%s,%.4f,%.4f,%.2f,%.2f,%.4f,%.4f",
                testName, x, y, hardness, modulus, o2, h2o);
    }
}