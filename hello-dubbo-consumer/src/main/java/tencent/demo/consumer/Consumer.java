package tencent.demo.consumer;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import tencent.demo.DemoService;

public class Consumer {
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"META-INF/spring/consumer.xml"});
        context.start();

        DemoService demoService = (DemoService)context.getBean("demoService");


        while(true) {
            try {
                String hello = demoService.sayHello("哈哈");
                System.out.println(hello);
            }catch (Exception ex){
                System.out.println("Provider 未准备好，等待吧。");
            }
            Thread.sleep(5 * 1000);
        }

//        new CountDownLatch(1).await();
    }
}
