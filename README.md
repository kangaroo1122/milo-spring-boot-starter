#milo-spring-boot-starter

milo 封装工具包，yml配置OPC UA地址，是否匿名等信息，即可连接OPC UA服务器

## 配置
```yaml
coctrl:
  milo:
    endpoint: opc.tcp://127.0.0.1:49320
    security-policy: none
```

```yaml
coctrl:
  milo:
    endpoint: opc.tcp://127.0.0.1:49320
    security-policy: basic256sha256
    username: OPCUA
    password: 123456
```
特别提醒：

在kepware中，用户名/密码访问时，opcua配置，安全策略中三个策略全部勾选

同时kepware选项属性中的OPC UA配置，不允许匿名访问

此时，security-policy可选值：basic256sha256，basic256，basic128rsa15都可

同时配置上 用户名/密码 即可访问服务器

## 使用
注入MiloService即可使用，支持：批量读、单个写（批量写，循环即可）、批量订阅（订阅不好使，推荐kepware使用MQTT实现订阅功能）

其中：写值是可能需要指定数据类型，视点位情况而定
```java
@SpringBootTest
@RunWith(SpringRunner.class)
public class MiloTest {
    @Autowired
    MiloService miloService;

    @Test
    public void writeToOpcUa() {
        miloService.writeToOpcUa(
                ReadOrWrite.builder()
                        .identifier("GA.T1.Boolean")
                        //Kep中是Boolean类型
                        .value(true)
                        .build());
        miloService.writeToOpcUa(
                ReadOrWrite.builder()
                        .identifier("GA.T1.LLong")
                        //Kep中是LLong类型，即：Int64，Java中的Long类型
                        .value(1235468L)
                        .build());
        miloService.writeToOpcUa(
                ReadOrWrite.builder()
                        .identifier("GA.T1.Long")
                        //Kep中是Long类型，即：Int32，Java中的int类型
                        .value(123456)
                        .build());
        miloService.writeToOpcUa(
                ReadOrWrite.builder()
                        .identifier("GA.T1.Short")
                        //Kep中是Short类型，即：Int16，Java中的short类型
                        .value((short) 123)
                        .build());
        miloService.writeToOpcUa(
                ReadOrWrite.builder()
                        .identifier("GA.T1.String")
                        .value("字符串")
                        .build());
        miloService.writeToOpcUa(
                ReadOrWrite.builder()
                        .identifier("GA.T1.Word")
                        //Kep中是Word类型，即：UInt16，无符号
                        .variant(new Variant(Unsigned.ushort("123")))
                        .build());
        miloService.writeToOpcUa(
                ReadOrWrite.builder()
                        .identifier("GA.T1.Float")
                        //Kep中是Float类型
                        .value(123.123F)
                        .build());
        miloService.writeToOpcUa(
                ReadOrWrite.builder()
                        .identifier("GA.T1.Double")
                        //Kep中是Double类型
                        .value(123.123)
                        .build());
    }
}
```
其他的数据类型，以此类推，.value(xxx) 写值失败，则需要转为自定义指定类型.variant(new Variant(xxx))

new Variant(xxx)：
> new Variant(String[])
> 
> new Variant(Unsigned.ushort("123"))
> 
> ....

参数类型具体以标签数据类型为准

两者同时存在，则以 .variant() 为准