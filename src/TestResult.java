// 这个类用来暂存单个测试点的数据
public class TestResult {
    public String testName; // 测试名称
    public double x;        // X坐标
    public double y;        // Y坐标
    public double hardness; // 硬度
    public double modulus;  // 模量

    public TestResult(String name, double x, double y, double h, double m) {
        this.testName = name;
        this.x = x;
        this.y = y;
        this.hardness = h;
        this.modulus = m;
    }

    // 方便把这一行转成 CSV 格式的字符串
    public String toCSVString() {
        return testName + "," + x + "," + y + "," + hardness + "," + modulus;
    }
}