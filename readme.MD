# Dubbo to Istio / Dubbo Mesh 极简改造指南

无需改造 dubbo sdk，无需更换 dubbo 协议，直接使用 istio 治理 dubbo 流量。这是史上最简单的改造方法。


## step 1. 改造 provider
去掉 registry 配置，让 provider 变成一个纯纯的 dubbo 应用。
修改配置，三个false，address 随便写一个字符串（因为他不能为空）:
```xml
<beans>
    <dubbo:application name="hello-dubbo-provider"/>
    <dubbo:protocol name="dubbo" port="20880" />
    <dubbo:registry register="false" subscribe="false" check="false" address="none" />
    <!--<dubbo:registry address="zookeeper://zk.dubbo:2181" client="curator" />-->
    <bean id="demoService" class="tencent.demo.provider.DemoServiceImpl"/>
    <dubbo:service interface="tencent.demo.DemoService" ref="demoService" />
</beans>
```
先在本地启动它。
## step 2. 改造 consumer
去掉 registry 配置，让 consumer 变成一个傻傻的 dubbo 应用。
手工指定 service 的服务地址，当然了，要把他变成你部署在 istio 中的**服务名** `url="dubbo://provider-service:20880"`。
```xml
<beans>
    <dubbo:application name="demo-consumer"/>
    <dubbo:registry address="none" register="false" subscribe="false" check="false" />
    <!--<dubbo:registry address="zookeeper://zk.dubbo:2181" client="curator" />-->
    <dubbo:reference id="demoService" check="false" interface="tencent.demo.DemoService" url="dubbo://provider-service:20880" />
</beans>
```
在 hosts 中映射一下。 
```
127.0.0.1 provider-service
```


启动 consumer，不出意外，完美运行。


上面的对 dubbo 的改造其实是在使用 dubbo 的调试功能，但他恰恰是 istio 需要的效果。
## step 3. 部署到 istio 中


在低版本的 istio 中，控制面无法解析。需要配置 EnvoyFilter。 但配了半天，完全不通 。干脆装个 1.6 玩玩。


经过实验，在 istio 1.6.1 中，基于 tcp 服务治理的 dubbo mesh 直接启动成功。


以下是实验过程：示例中的镜像地址目前是公开可测的。


### 1 创建 ns
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: dubbo
  labels:
    istio-injection: enabled
spec:
  finalizers:
    - kubernetes
```


### 2 创建 provider 的部署集
```yaml

apiVersion: apps/v1
kind: Deployment
metadata:
  name: hello-dubbo-provider
  namespace: dubbo
  labels:
    app: hello-dubbo-provider
    version: v1
spec:
  replicas: 1
  selector:
    matchLabels:
      app: hello-dubbo-provider
      version: v1
  template:
    metadata:
      labels:
        app: hello-dubbo-provider
        version: v1
    spec:
      containers:
        - name: hello-dubbo-provider
          image: tencent-cloud-one-docker.pkg.coding.net/xyz-demo/images/hello-dubbo-provider:1.0.4
          command: ["java","-jar","hello-dubbo-provider-fat.jar"]
          ports:
            - containerPort: 20880
              protocol: TCP
---
# 部署 hello-dubbo-provider v2
apiVersion: apps/v1
kind: Deployment
metadata:
  name: hello-dubbo-provider-v2
  namespace: dubbo
  labels:
    app: hello-dubbo-provider
    version: v2
spec:
  replicas: 1
  selector:
    matchLabels:
      app: hello-dubbo-provider
      version: v2
  template:
    metadata:
      labels:
        app: hello-dubbo-provider
        version: v2
    spec:
      containers:
        - name: hello-dubbo-provider
          image: tencent-cloud-one-docker.pkg.coding.net/xyz-demo/images/hello-dubbo-provider:2.0.0
          command: ["java","-jar","hello-dubbo-provider-fat.jar"]
          ports:
            - containerPort: 20880
              protocol: TCP
```


### 创建 service
```yaml

apiVersion: v1
kind: Service
metadata:
  name: hello-dubbo-provider
  namespace: dubbo
  labels:
    name: hello-dubbo-provider
spec:
  ports:
  - name: dubbo-rpc
    protocol: TCP
    port: 20880
    targetPort: 20880
  selector:
    app: hello-dubbo-provider
```


### 创建 consumer 部署
```yaml

apiVersion: apps/v1
kind: Deployment
metadata:
  name: hello-dubbo-consumer
  namespace: dubbo
  labels:
    app: hello-dubbo-consumer
    version: v1
spec:
  replicas: 10
  selector:
    matchLabels:
      app: hello-dubbo-consumer
      version: v1
  template:
    metadata:
      labels:
        app: hello-dubbo-consumer
        version: v1
    spec:
      containers:
        - name: hello-dubbo-consumer
          image: tencent-cloud-one-docker.pkg.coding.net/xyz-demo/images/hello-dubbo-consumer:1.0.3
          command: ["java","-jar","hello-dubbo-consumer-fat.jar"]
```


### 流量/灰度控制
```yaml

apiVersion: networking.istio.io/v1alpha3
kind: VirtualService
metadata:
  name: hello-vs
  namespace: dubbo
spec:
  hosts:
    - hello-dubbo-provider
  tcp:
    - route:
        - destination:
            host: hello-dubbo-provider
            port:
              number: 20880
            subset: v1
          weight: 20
        - destination:
            host: hello-dubbo-provider
            port:
              number: 20880
            subset: v2
          weight: 80
---
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: hello-subset
  namespace: dubbo
spec:
  host: hello-dubbo-provider
  subsets:
    - name: v1
      labels:
        version: v1
    - name: v2
      labels:
        version: v2
```


### 测试结果
部署完成，查看 consumer 的各个 pod 的 log。会发现，少量 consumer 打印了 hello-dubbo-provider:v1 的结果。
```shell
2020-06-10T10:58:22.870417943Z Hello 哈哈, V1 @ 2020-06-10 10:58:22 @ 172.24.0.136
2020-06-10T10:58:27.872374594Z Hello 哈哈, V1 @ 2020-06-10 10:58:27 @ 172.24.0.136
2020-06-10T10:58:32.874072253Z Hello 哈哈, V1 @ 2020-06-10 10:58:32 @ 172.24.0.136
```
v2 的结果的比较多：
```shell
2020-06-10T11:00:32.625814149Z 你好世界， 版本 2 升级了这个提示 哈哈, V2 @ 2020-06-10 11:00:32 @ 172.24.0.7
2020-06-10T11:00:37.638914148Z 你好世界， 版本 2 升级了这个提示 哈哈, V2 @ 2020-06-10 11:00:37 @ 172.24.0.7
2020-06-10T11:00:42.635862572Z 你好世界， 版本 2 升级了这个提示 哈哈, V2 @ 2020-06-10 11:00:42 @ 172.24.0.7
```
调整 vs 的百分比，重启 pod（因为长连接，所以重启），看看他是不是按照指挥来调度。


另外可以使用如下的命令多次执行 consumer 进行验证：
```shell
kubectl exec -n dubbo hello-dubbo-consumer-855fcb7878-5qcb4 -- java -jar hello-dubbo-consumer-fat.jar
```


在 kiali 中，也能看到流量的监控图表。


### 附：dubbo 的 java 代码


Provider。版本 1 和 2 分别打印了不同的内容。
```java
    public String sayHello(String name) {
        String myHost = "";
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            myHost = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        Date date = new Date();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        String str = format.format(date);
        return "你好世界， 版本 2 升级了这个提示 " + name + ", V2 @ " + str + " @ " + myHost ;
    }
```
consumer 每 5 秒调用一次：
```java
    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] {"META-INF/spring/consumer.xml"});
        context.start();
        DemoService demoService = (DemoService)context.getBean("demoService");
        while(true) {
            String hello = demoService.sayHello("哈哈");
            System.out.println(hello);
            Thread.sleep(5 * 1000);
        }
    }
```
## 总结
使用了最简单的方法，将 dubbo 的服务发现和治理功能去掉，保留了他的 rpc 功能，并通过与 istio 一致的部署方法进行服务部署和 TCP 流量操控。
但现在基于 istio 本身的调用链还没有看到，基于内容的流量操控还待继续探索。



---

参考：
[dubbo:registry]()
[dubbo:reference](http://dubbo.apache.org/zh-cn/docs/user/references/xml/dubbo-reference.html)



