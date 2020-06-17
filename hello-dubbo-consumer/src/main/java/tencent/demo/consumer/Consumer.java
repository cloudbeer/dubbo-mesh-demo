package tencent.demo.consumer;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import tencent.demo.DemoService;

import java.util.concurrent.CountDownLatch;

public class Consumer {
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"META-INF/spring/consumer.xml"});
        context.start();
        // Obtaining a remote service proxy
        DemoService demoService = (DemoService)context.getBean("demoService");
        // Executing remote methods

        while(true) {
            String hello = demoService.sayHello("啤酒云");
            String reject = demoService.sayReject("黑客");
            // Display the call result
            System.out.println(hello);
            System.out.println(reject);
            Thread.sleep(5 * 1000);
        }

//        new CountDownLatch(1).await();
    }
}
