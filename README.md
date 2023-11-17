# ToYou - Backend

## 配置支付

支持的支付

- 支付宝
- 微信

### 支付宝

1. 在`application.properties`中配置`pay.alipay.app-id`为支付宝分配的`APPID`
2. 在项目根目录创建`profile`文件夹，在内部创建`alipay`文件夹，在文件夹内部放入如下表格的文件。

| 描述      | 文件名                       | 模式   |
|---------|---------------------------|------|
| 公钥      | publicKey.txt             | 密钥模式 |
| 私钥      | privateKey.txt            | 必须   |
| 应用公钥证书  | appCertPublicKey.crt      | 证书模式 |
| 支付宝公钥证书 | alipayCertPublicKey.crt   | 证书模式 |
| 支付宝根证书  | alipay/alipayRootCert.crt | 证书模式 |

### 微信

1. 在`application.properties`中配置`pay.wxpay.app-id`为微信分配的`APPID`