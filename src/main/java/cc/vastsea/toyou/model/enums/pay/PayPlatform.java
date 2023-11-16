package cc.vastsea.toyou.model.enums.pay;

import lombok.Getter;

@Getter
public enum PayPlatform {
	//支付宝扫码
	ALIPAY(1, "支付宝(1)"),
	// 支付宝跳转支付
	ALIPAY_REDIRECT(2, "支付宝(2)"),
	WECHAT(3, "微信(1)");
	final int code;
	final String desc;

	PayPlatform(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	public static PayPlatform of(int code) {
		for (PayPlatform payPlatform : PayPlatform.values()) {
			if (payPlatform.code == code) {
				return payPlatform;
			}
		}
		return ALIPAY;
	}
}
