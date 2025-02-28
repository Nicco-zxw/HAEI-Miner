import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

public class MainTestHAEIMiner {
    public static void main(String arg[]) throws IOException {
        //写入文件的位置
        String input1 = fileToPath("DBUtility.txt");
        String input2 = fileToPath("stock.txt");
        //输出文件的位置
        String output = ".//output.txt";

        //最小阈值
        double min_AEutility = 160;
        //运行HAEIMiner算法
        AlgoHAEIMiner haeim = new AlgoHAEIMiner();
        haeim.runAlgorithm(input1, input2, output, min_AEutility);
        //输出运行状态
        haeim.printStats();
    }

    // 从文件获取路径
    public static String fileToPath(String filename) throws UnsupportedEncodingException {
        URL url = MainTestHAEIMiner.class.getResource(filename);
        return java.net.URLDecoder.decode(url.getPath(),"UTF-8");
    }

}
