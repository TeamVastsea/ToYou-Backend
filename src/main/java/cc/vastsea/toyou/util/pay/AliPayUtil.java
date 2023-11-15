package cc.vastsea.toyou.util.pay;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class AliPayUtil {
	private final String domain;

	private final String appId;

	private final String privateKey;

	private final String publicKey;

	public AliPayUtil(String domain, String appId, String privateKey, String publicKey) {
		this.domain = domain;
		this.appId = appId;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		Factory.setOptions(getOptions(false));
	}

	private Config getOptions(boolean certificate) {
		Config config = new Config();
		config.protocol = "https";
		config.gatewayHost = "openapi.alipay.com";
		config.signType = "RSA2";

		config.appId = appId;

		// 为避免私钥随源码泄露，推荐从文件中读取私钥字符串而不是写入源码中
		config.merchantPrivateKey = privateKey;

		if (certificate) {
			//注：证书文件路径支持设置为文件系统中的路径或CLASS_PATH中的路径，优先从文件系统中加载，加载失败后会继续尝试从CLASS_PATH中加载
			config.merchantCertPath = "./foo/alipay/appCertPublicKey.crt";
			config.alipayCertPath = "./foo/alipay/alipayCertPublicKey.crt";
			config.alipayRootCertPath = "./foo/alipay/alipayRootCert.crt";
		} else {
			//注：如果采用非证书模式，则无需赋值上面的三个证书路径，改为赋值如下的支付宝公钥字符串即可
			config.alipayPublicKey = publicKey;
		}

		config.notifyUrl = domain + "/pay/aliPay";

		return config;
	}
}
