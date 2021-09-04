#milo-spring-boot-starter

milo 封装工具包，yml配置OPC UA地址，是否匿名等信息，即可连接OPC UA服务器

```yaml
coctrl:
  milo:
    anonymous: true
    endpoint: opc.tcp://127.0.0.1:49320
```

注入MiloService即可使用，支持：批量读、单个写（批量写，循环即可）、批量订阅（订阅不好使，推荐kepware使用MQTT实现订阅功能）
