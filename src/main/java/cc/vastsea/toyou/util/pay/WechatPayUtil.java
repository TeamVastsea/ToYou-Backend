package cc.vastsea.toyou.util.pay;

import cc.vastsea.toyou.model.enums.pay.TradeStatus;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public class WechatPayUtil {
	private final String domain;
	/**
	 * 商户号
	 */
	private final String merchantId;
	/**
	 * 商户证书序列号
	 */
	private final String merchantSerialNumber;
	private final String apiV2Key;
	/**
	 * 商户APIV3密钥
	 */
	private final String apiV3Key;
	private final RSAAutoCertificateConfig config;

	public WechatPayUtil(String domain, String merchantId, String merchantSerialNumber, String apiV2Key, String apiV3Key) {
		this.domain = domain;
		this.merchantId = merchantId;
		this.merchantSerialNumber = merchantSerialNumber;
		this.apiV2Key = apiV2Key;
		this.apiV3Key = apiV3Key;
		String privateKeyPath = "./profile/wechat/apiclient_key.pem";
		// 使用自动更新平台证书的RSA配置
		// 一个商户号只能初始化一个配置，否则会因为重复的下载任务报错
		this.config = new RSAAutoCertificateConfig.Builder()
				.merchantId(merchantId)
				.privateKeyFromPath(privateKeyPath)
				.merchantSerialNumber(merchantSerialNumber)
				.apiV3Key(apiV3Key)
				.build();
	}

	// 转换
	public static TradeStatus getTradeStatus(Transaction.TradeStateEnum stateEnum) {
		return switch (stateEnum) {
			case SUCCESS -> TradeStatus.SUCCESS;
			case REFUND -> TradeStatus.REFUND;
			case NOTPAY -> TradeStatus.NOTPAY;
			case CLOSED -> TradeStatus.CLOSED;
			default -> TradeStatus.UNKNOWN;
		};
	}
}