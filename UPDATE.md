## 3.0.2 

- 订阅时 支持指定订阅时间间隙，默认 1000ms

## 3.0.1

- endpoint 支持外网穿透类的地址
- 新增点位订阅方法，订阅断掉自动重连

> 外网穿透类地址如：
> - 内网地址为：opc.tcp://192.168.68.128:49320
> - 外网地址为：opc.tcp://opc.kangaroohy.com:59320

## 3.0.0

- 适配 spring boot 3.x，也可在spring boot 2.x中使用
- 基于 eclipse milo 最新版本 0.6.9
- 支持连接池配置
- 首次封装，提供以下方法

![img_1.png](screenshot/img_1.png)
![img_2.png](screenshot/img_2.png)
![img_3.png](screenshot/img_3.png)
