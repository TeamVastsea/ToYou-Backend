package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.service.PayService;
import cc.vastsea.toyou.util.pay.AliPayUtil;
import cc.vastsea.toyou.util.pay.PaymentUtil;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.util.ResponseChecker;
import com.alipay.easysdk.payment.page.models.AlipayTradePagePayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PayServiceImpl implements PayService {
	@Value("${domain:http://localhost:8102}")
	private String domain;

	@Value("${pay.alipay.app-id:APP-ID}")
	private String appId;

	@Value("${pay.alipay.private-key:private-key}")
	private String privateKey;

	@Value("${pay.alipay.public-key:public-key}")
	private String publicKey;
	private AliPayUtil aliPayUtil = null;

	public void init() {
		if (aliPayUtil == null) {
			aliPayUtil = new AliPayUtil(domain, appId, privateKey, publicKey);
		}
	}

	@Override
	public String test() {
		init();
		String tradeNo = PaymentUtil.generateTradeNumber();
		String returnUrl = aliPayUtil.getDomain() + "/pay/aliPay";
		try {
			AlipayTradePagePayResponse response = Factory.Payment.Page().pay("test", tradeNo, "1.00", returnUrl);
			// 3. 处理响应或异常
			if (ResponseChecker.success(response)) {
				return response.getBody();
			}
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "调用失败");
		} catch (Exception e) {
			log.error("调用遭遇异常，原因：" + e.getMessage());
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "调用遭遇异常");
		}
	}
}
