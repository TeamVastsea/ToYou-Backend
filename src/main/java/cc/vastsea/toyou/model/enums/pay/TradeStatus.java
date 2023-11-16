package cc.vastsea.toyou.model.enums.pay;

import lombok.Getter;

@Getter
public enum TradeStatus {
	NOTPAY(0, "未支付"),
	SUCCESS(1, "支付成功"),
	REFUND(2, "转入退款"),
	CLOSED(3, "已关闭"),
	UNKNOWN(4, "未知状态");
	final int code;
	final String desc;

	TradeStatus(int code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	public static TradeStatus of(int code) {
		for (TradeStatus tradeStatus : TradeStatus.values()) {
			if (tradeStatus.code == code) {
				return tradeStatus;
			}
		}
		return NOTPAY;
	}
}
