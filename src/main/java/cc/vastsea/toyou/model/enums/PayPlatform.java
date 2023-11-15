package cc.vastsea.toyou.model.enums;

public enum PayPlatform {
	ALIPAY(1, "支付宝"),
	WECHAT(2, "微信");
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
