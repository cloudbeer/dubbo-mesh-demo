package tencent.demo.provider;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.CountDownLatch;

public class Provider {
    public static void main(String[] args) throws Exception {
        System.setProperty("java.net.preferIPv4Stack", "true");
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext(new String[]{"spring/provider.xml"});
        context.start();
        System.out.println("Provider started.");
        new CountDownLatch(1).await();
    }
}
