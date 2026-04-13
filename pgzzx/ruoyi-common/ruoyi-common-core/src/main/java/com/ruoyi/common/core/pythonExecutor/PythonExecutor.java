package com.ruoyi.common.core.pythonExecutor;

//import com.alibaba.fastjson2.JSONObject;
//import org.python.core.*;
//import org.python.util.PythonInterpreter;
//
//import java.io.*;
//
//public class PythonExecutor{
//
//    public static PyObject pythonExecutor(String algorithmPath, String inputData){
//        try {
//            PythonInterpreter interpreter = new PythonInterpreter();
//            interpreter.execfile(algorithmPath);
//
//            // 第一个参数为期望获得的函数（变量）的名字，第二个参数为期望返回的对象类型
//            PyFunction pyFunction = interpreter.get("main", PyFunction.class);
//
//            //传入参数为String类型时
//            PyString strJson = Py.newStringUTF8(inputData);
//
//            //传入参数为字典时
////        String jsonStr = parseJsonFile(new File("C:\\Users\\Desktop\\能源分系统指标体系.json"));
////        System.out.println(jsonStr);
////        PyString strJson= Py.newStringUTF8(jsonStr);
////        System.out.println(strJson);
//
//            //调用函数，如果函数需要参数，在Java中必须先将参数转化为对应的“Python类型”
//            PyObject pyobj = pyFunction.__call__(strJson);
//            System.out.println("the anwser is: " + pyobj);
//            return pyobj;
//        }catch(Exception e) {
//            System.out.println(e.toString());
//            return null;
//        }
//    }
//
//    public String parseJsonFile(File file) throws IOException {
//        FileReader fileReader = new FileReader(file);
//        Reader reader = new InputStreamReader(new FileInputStream(file), "Utf-8");
//        int ch = 0;
//        StringBuilder sb = new StringBuilder();
//        while ((ch = reader.read()) != -1) {
//            sb.append((char) ch);
//        }
//        fileReader.close();
//        reader.close();
//        return sb.toString();
//    }
//
//}




import com.alibaba.fastjson2.JSONObject;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * java调用python的执行器
 */
@Component
public class PythonExecutor {

    private static final Logger logger = LoggerFactory.getLogger(PythonExecutor.class);
    private static final String OS = System.getProperty("os.name");

//    private static final String WINDOWS_PATH = ClassUtils.getDefaultClassLoader().getResource("").getPath().substring(1) + "py/automl/";  // windows为获取项目根路径即可
    private static final String WINDOWS_PATH = "";  // windows为获取项目根路径即可

    private static final String LINUX_PATH = "/ai/xx";// linux为python文件所在目录

    private static final ExecutorService taskPool = new ThreadPoolExecutor(3, 3
            , 200L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(600)
            , new ThreadFactoryBuilder()
            .setNameFormat("thread-自定义线程名-runner-%d").build());

    /**
     * 执行python文件【异步 无需等待py文件执行完毕】
     *
     * @param fileName python文件地址
     * @param params   参数
     * @throws IOException
     */
//    public static void execPythonFile(String fileName, String params) {
//        taskPool.submit(() -> {
//            try {
//                exec(fileName, params);
//            } catch (IOException e) {
//                logger.error("读取python文件 fileName=" + fileName + " 异常", e);
//            }
//        });
//
//    }

    /**
     * 执行python文件 【同步 会等待py执行完毕】
     *
     * @param fileName python文件地址
     * @param params   参数
     * @throws IOException
     */
    public static List<String> execPythonFileSync(String fileName, String params) {
        try {
            return execSync(fileName, params);
        } catch (IOException e) {
            logger.error("读取python文件 fileName=" + fileName + " 异常", e);
            return null;
        }
    }

    private static List<String> exec(String fileName, String params) throws IOException {
        logger.info("读取python文件 init fileName={}&path={}", fileName, WINDOWS_PATH);
        Process process;
        final String[] successList = {new String()};
        final String[] errorList = {new String()};
        if (OS.startsWith("Windows")) {
            // windows执行脚本需要使用 cmd.exe /c 才能正确执行脚本
//            String cmd = "cmd.exe /c python " + fileName + params;
//            Runtime.getRuntime();
            process = new ProcessBuilder("cmd.exe", "/c", "python", WINDOWS_PATH + fileName, params).start();
        } else {
            // linux执行脚本一般是使用python3 + 文件所在路径
            process = new ProcessBuilder("python3", LINUX_PATH + fileName, params).start();
        }

        new Thread(() -> {
            logger.info("读取python文件 开始 fileName={}", fileName);
            BufferedReader errorReader = null;
            // 脚本执行异常时的输出信息
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            List<String> errorString = read(fileName, errorReader);
            for(int i=0;i<errorString.size();i++){
                errorList[i] = errorString.get(i);
            }
            logger.info("读取python文件 异常 fileName={}&errorString={}", fileName, errorString);
        }).start();


        new Thread(() -> {
            // 脚本执行正常时的输出信息
            BufferedReader inputReader = null;
            inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            List<String> returnString = read(fileName, inputReader);
            for(int i=0;i<returnString.size();i++){
                successList[i] = returnString.get(i);
            }
            logger.info("读取python文件 fileName={}&returnString={}", fileName, returnString);
        }).start();

        try {
            logger.info("读取python文件 wait fileName={}", fileName);
            process.waitFor();
            return Arrays.asList(successList);
        } catch (InterruptedException e) {
            logger.error("读取python文件 fileName=" + fileName + " 等待结果返回异常", e);
            return Arrays.asList(errorList);
        }
//        logger.info("读取python文件 fileName={} == 结束 ==", fileName);

    }

    private static List<String> execSync(String fileName, String params) throws IOException {
        logger.info("同步读取python文件 init fileName={}&path={}", fileName, WINDOWS_PATH);
        Process process;
        final String[] successList = {new String()};
        final String[] errorList = {new String()};
        if (OS.startsWith("Windows")) {
            // windows执行脚本需要使用 cmd.exe /c 才能正确执行脚本
            process = new ProcessBuilder("cmd.exe", "/c", "python", WINDOWS_PATH + fileName, params).start();
//            String cmd = "python " + fileName + " " + params;
//            process = Runtime.getRuntime().exec(cmd);
        } else {
            // linux执行脚本一般是使用python3 + 文件所在路径
            process = new ProcessBuilder("python3", LINUX_PATH + fileName, params).start();
        }


        taskPool.submit(() -> {
//            SequenceInputStream sis = new SequenceInputStream(process.getErrorStream(),process.getInputStream());
//            InputStreamReader isr = null;
//            try {
//                isr = new InputStreamReader(sis,"utf-8");
//            } catch (UnsupportedEncodingException e) {
//                e.printStackTrace();
//            }
//            BufferedReader br = new BufferedReader(isr);
//            List<String> returnString = read(fileName, br);
//            for(int i=0;i<returnString.size();i++){
//                successList[i] = returnString.get(i);
//            }


            logger.info("读取python文件 开始 fileName={}", fileName);
            BufferedReader errorReader = null;

            // 脚本执行异常时的输出信息
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            List<String> errorString = read(fileName, errorReader);
            for(int i=0;i<errorString.size();i++){
                errorList[i] = errorString.get(i);
            }
//            try {
//                errorReader.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            logger.info("读取python文件 异常 fileName={}&errorString={}", fileName, errorString);
        });

        taskPool.submit(() -> {
            // 脚本执行正常时的输出信息
            BufferedReader inputReader = null;

            inputReader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            List<String> returnString = read(fileName, inputReader);

            for(int i=0;i<returnString.size();i++){
                successList[i] = returnString.get(i);
            }
//            try {
//                inputReader.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
            logger.info("读取python文件 fileName={}&returnString={}", fileName, returnString);
        });

        try {
            logger.info("同步读取python文件 wait fileName={}", fileName);
            process.waitFor();
//            taskPool.shutdown();
//            taskPool.awaitTermination(30000, TimeUnit.MILLISECONDS);
            return Arrays.asList(successList);
        } catch (InterruptedException e) {
            logger.error("同步读取python文件 fileName=" + fileName + " 等待结果返回异常", e);
//            taskPool.shutdown();
            return Arrays.asList(errorList);
        }
//        logger.info("同步读取python文件 fileName={} == 结束 ==", fileName);
    }

    private static List<String> read(String fileName, BufferedReader reader) {
        List<String> resultList = new ArrayList<>();
        String res = "";
        while (true) {
//            try {
//                Thread.sleep(300);
//            }catch (Exception e){
//                e.printStackTrace();
//            }
            try {
                if (((res = reader.readLine()) == null)) {
                    break;
                }
            } catch (IOException e) {
                logger.error("读取python文件 fileName=" + fileName + " 读取结果异常", e);
            }
            resultList.add(res);
        }
        return resultList;
    }

    public static String parseJsonFile(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
        int ch = 0;
        StringBuilder sb = new StringBuilder();
        while ((ch = reader.read()) != -1) {
            sb.append((char) ch);
        }
        fileReader.close();
        reader.close();
        return sb.toString();
    }
    public static String str2HexStr(String str) {
        char[] chars = "0123456789ABCDEF".toCharArray();
        StringBuilder sb = new StringBuilder("");
        byte[] bs = str.getBytes();
        int bit;
        for (int i = 0; i < bs.length; i++) {
            bit = (bs[i] & 0x0f0) >> 4;
            sb.append(chars[bit]);
            bit = bs[i] & 0x0f;
            sb.append(chars[bit]);
            // sb.append(' ');
        }
        return sb.toString().trim();
    }

    public static String hexStr2str(String str) {
        byte[] bytes = new byte[str.length() / 2];
        byte tempByte = 0;
        byte tempHigh = 0;
        byte tempLow = 0;
        for (int i = 0, j = 0; i < str.length(); i += 2, j++) {
            tempByte = (byte) (((int) str.charAt(i)) & 0xff);
            if (tempByte >= 48 && tempByte <= 57) {
                tempHigh = (byte) ((tempByte - 48) << 4);
            } else if (tempByte >= 97 && tempByte <= 101) {
                tempHigh = (byte) ((tempByte - 97 + 10) << 4);
            }
            tempByte = (byte) (((int) str.charAt(i + 1)) & 0xff);
            if (tempByte >= 48 && tempByte <= 57) {
                tempLow = (byte) (tempByte - 48);
            } else if (tempByte >= 97 && tempByte <= 101) {
                tempLow = (byte) (tempByte - 97 + 10);
            }
            bytes[j] = (byte) (tempHigh | tempLow);
        }
        String result = null;
        result = new String(bytes, StandardCharsets.UTF_8);
        return result;
    }

    public static void main(String[] args) {
        try {
//            System.out.println(jsonObject);
//            String str = "7b227472656544617461223a207b226c6162656c223a20225c75363833395c75383238325c7537306239222c20226964223a202230222c20226368696c6472656e223a205b7b226c6162656c223a20225c75376562385c7535653031222c20226964223a202266383364343266662d313430362d346534392d623434322d373466653833613734656661222c20226368696c6472656e223a205b7b226c6162656c223a20225c75353461395c75353461395c75353461395c7535346139222c20226964223a202231653638663930612d633434302d343966312d623462372d666330623137623335623731222c20226368696c6472656e223a205b5d2c2022776569676874223a20312e307d5d2c2022776569676874223a20302e383534373336393330323035303634377d2c207b226c6162656c223a20225c75363330375c75363830373132333132333132222c20226964223a202236386338663866652d643262642d343836622d386631382d663761313862303238643232222c20226368696c6472656e223a205b7b226c6162656c223a20225c75366333385c75386664635c75366333385c7538666463222c20226964223a202232343538363934652d323536322d343162362d613236382d386531393536353833626632222c20226368696c6472656e223a205b5d2c2022776569676874223a20302e383639383039383938393434353832337d2c207b226c6162656c223a20223132313331222c20226964223a202232636533353330652d376330662d346637362d613830302d363365636133626662316633222c20226368696c6472656e223a205b5d2c2022776569676874223a20302e31333031393031303130353534313737337d5d2c2022776569676874223a20302e31343532363330363937393439333532387d5d2c2022776569676874223a20317d7d";
            List<Integer> list1 = new ArrayList<>();
            list1.add(1);
            list1.add(2);
            List<Integer> list2 = new ArrayList<>();
            list2.add(3);
            list2.add(4);
            List<List<Integer>> list = new ArrayList<>();
            list.add(list1);
            list.add(list2);
            List<String> returnList= execPythonFileSync("C:\\Users\\thinker\\Desktop\\cal_weight\\wxwx.py", "5 " + list.toString());
            System.out.println(returnList);
//            System.out.println(returnList.size());
//            System.out.println(returnList.get(1));
//            System.out.println(Charset.defaultCharset());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}