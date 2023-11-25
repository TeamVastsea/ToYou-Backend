package cc.vastsea.toyou.model.enums;

import lombok.Getter;

@Getter
public enum OAuthPlatForm {
	WECHAT(1, "微信"),
	QQ(2, "QQ"),
	GITHUB(3, "GitHub"),
	ALIPAY(4, "支付宝");
	final int code;
	final String desc;

	OAuthPlatForm(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	public static OAuthPlatForm of(int code) {
		for (OAuthPlatForm payPlatform : OAuthPlatForm.values()) {
			if (payPlatform.code == code) {
				return payPlatform;
			}
		}
		return null;
	}
}
