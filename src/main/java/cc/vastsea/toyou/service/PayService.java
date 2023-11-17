package cc.vastsea.toyou.service;

import cc.vastsea.toyou.model.enums.Group;
import cc.vastsea.toyou.model.enums.pay.PayPlatform;
import cc.vastsea.toyou.util.pay.AliPayUtil;
import cc.vastsea.toyou.util.pay.WechatPayUtil;
import jakarta.servlet.http.HttpServletRequest;

public interface PayService {
	AliPayUtil getAliPayUtil();

	WechatPayUtil getWechatPayUtil();

	String createOrder(long uid, Group group, int month, PayPlatform payPlatform, String returnUrl, HttpServletRequest request);
}
