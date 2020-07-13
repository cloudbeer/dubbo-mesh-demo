package tencent.demo.provider;

import tencent.demo.DemoService;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class DemoServiceImpl implements DemoService {

    public String sayHello(String name) {
        String myHost = "";
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            myHost = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


        String namespace = System.getenv("ns");

        if (namespace == null) {
            namespace = "未知";
        }


//        Date date = new Date();
//        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
//        String str = format.format(date);
        return "你好：" + name + ", V2 来自 " + namespace + " 空间 @ " + myHost;
    }
}
