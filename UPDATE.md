## 3.2.1.0.6.16 & 4.0.1.1.1.7

- 优化多 endpoint 连接和订阅生命周期，修复并发关闭问题。
- 优化订阅回调队列，增加积压、丢弃和失败统计。
- 完善写入参数校验，增加逐点写入结果和 NodeId 浏览接口。
- 增加可配置的应用级证书管理，移除旧的静态证书加载器。
- 移除未使用的异步超时工具类。

## 4.0.0.1.1.7

- 最低运行环境升级为 Java 17，支持 Spring Boot 3.x、4.x，不再支持 Spring Boot 2.x。⚠️具有破坏性。
- Eclipse Milo 升级到 1.1.7，依赖切换为 `milo-sdk-client`。
- 订阅实现迁移到 Milo 1.x 的 `OpcUaSubscription`、`OpcUaMonitoredItem` API，继续复用 endpoint 连接和服务端 Subscription。
- 证书信任列表迁移到 Milo 1.x 文件式信任列表和拒绝证书隔离区。
- 读写调用、身份认证、浏览 API 和 Spring Boot 自动配置注册方式同步升级。
- `SubscriptionCallback` 的监控项参数由 `ManagedDataItem` 改为 `OpcUaMonitoredItem`。⚠️具有破坏性。

## 3.2.0.0.6.16

- 升级 Eclipse Milo 到 0.6.16。
- 移除 OPC UA 连接池，改为每个 endpoint 一个长期客户端，读写和订阅共享连接。⚠️具有破坏性。
- 订阅按 endpoint、发布周期和采样周期复用服务端 Subscription，支持取消订阅和断线重建。
- 批量读改为按服务端能力分片的 OPC UA Read 请求，修复浏览递归重复访问和节点图循环问题。
- 读写和监控项创建增加分片、超时、失败回滚；订阅回调改为可配置的有界条带线程池。
- 修复订阅恢复失败误触发其他 Subscription 重建的问题。
- 修复订阅重建时旧监控项监听器残留问题；移除旧的 SubscriptionRunner，统一使用订阅管理器。⚠️具有破坏性。

## 3.1.4.0.6.15

- 支持代码加载连接，实现`MiloConfigProvider`，`@Component` 给spring管理即可

## 3.1.2.0.6.15

- 升级到最新版本 0.6.15
- 修复长时间运行导致缓存文件过多的问题
- `readFromOpcUa` 读取数值时支持设置超时时间，默认 `10000ms`

## 3.1.1.0.6.13

- 读值支持返回更多信息 [#PR16](https://github.com/kangaroo1122/milo-spring-boot-starter/pull/16)

## 3.1.0.6.13

- 升级到最新版本 0.6.13
- 修改订阅监听callback参数以获取更多信息，具有破坏性！！！[#PR15](https://github.com/kangaroo1122/milo-spring-boot-starter/pull/15)

## 3.0.6.12

- 升级到最新版本 0.6.12
- 连接池配置优化 [#gitee I86XSX](https://gitee.com/vampire001/milo-spring-boot-starter/issues/I86XSX)

## 3.0.5

- 支持配置是否启用组件：`kangaroohy.milo.enabled=true`

## 3.0.4

- 支持配置多个 opc ua 服务器，调用方法时可手动指定需要访问的服务器
- 批量读值方法优化
- 升级milo依赖到 0.6.10

## 3.0.3

- ID支持字符串表示法：ns=<命名空间索引>;<标识符类型>=<标识符>

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
