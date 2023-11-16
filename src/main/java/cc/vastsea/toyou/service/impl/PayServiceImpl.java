package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.mapper.OrderMapper;
import cc.vastsea.toyou.model.entity.Order;
import cc.vastsea.toyou.model.enums.Group;
import cc.vastsea.toyou.model.enums.pay.PayPlatform;
import cc.vastsea.toyou.service.PayService;
import cc.vastsea.toyou.util.pay.AliPayUtil;
import cc.vastsea.toyou.util.pay.PaymentUtil;
import cc.vastsea.toyou.util.pay.WechatPayUtil;
import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.util.ResponseChecker;
import com.alipay.easysdk.payment.facetoface.models.AlipayTradePrecreateResponse;
import com.alipay.easysdk.payment.page.models.AlipayTradePagePayResponse;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PayServiceImpl implements PayService {
	@Resource
	private OrderMapper orderMapper;

	@Value("${domain:http://localhost:8102}")
	private String domain;

	@Value("${pay.alipay.app-id:APP-ID}")
	private String aliPayAppId;

	@Value("${pay.wechat.app-id:app-id}")
	private String wechatAppId;

	@Value("${pay.wechat.merchant-id:merchant-id}")
	private String wechatMerchantId;

	@Value("${pay.wechat.merchant-serial-number:merchant-serial-number}")
	private String wechatMerchantSerialNumber;

	@Value("${pay.wechat.api-v2-key:api-v2-key}")
	private String apiV2Key;

	@Value("${pay.wechat.api-v3-key:api-v3-key}")
	private String apiV3Key;

	private AliPayUtil aliPayUtil = null;
	private WechatPayUtil wechatPayUtil = null;

	public void init() {
		if (aliPayUtil == null) {
			aliPayUtil = new AliPayUtil(domain, aliPayAppId);
		}
		if (wechatPayUtil == null) {
			wechatPayUtil = new WechatPayUtil(domain, wechatMerchantId, wechatMerchantSerialNumber, apiV2Key, apiV3Key);
		}
	}

	@Override
	public String alipayTest() {
		init();
		String tradeNo = PaymentUtil.generateTradeNumber();
		String returnUrl = aliPayUtil.getDomain() + "/pay/aliPay";
		try {
			AlipayTradePagePayResponse response = Factory.Payment.Page().pay("test", tradeNo, "0.01", returnUrl);
			// AlipayTradePrecreateResponse response = Factory.Payment.FaceToFace().preCreate("test", tradeNo, "0.01");
			// 3. 处理响应或异常
			if (ResponseChecker.success(response)) {
				// return response.getQrCode();
				return response.getBody();
			}
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "调用失败");
		} catch (Exception e) {
			log.error("调用遭遇异常，原因：" + e.getMessage());
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "调用遭遇异常");
		}
	}

	@Override
	public String wechatTest() {
		init();
		String tradeNo = PaymentUtil.generateTradeNumber();
		// 构建service
		NativePayService service = new NativePayService.Builder().config(wechatPayUtil.getConfig()).build();
		// request.setXxx(val)设置所需参数，具体参数可见Request定义
		PrepayRequest request = new PrepayRequest();
		Amount amount = new Amount();
		amount.setTotal(1);
		request.setAmount(amount);
		request.setAppid(wechatAppId);
		request.setMchid(wechatMerchantId);
		request.setDescription("测试商品标题");
		String notifyUrl = domain + "/pay/wechat";
		request.setNotifyUrl(notifyUrl);
		request.setOutTradeNo(tradeNo);
		// 调用下单方法，得到应答
		PrepayResponse response = service.prepay(request);
		// 使用微信扫描 code_url 对应的二维码，即可体验Native支付
		return response.getCodeUrl();
	}

	@Override
	public AliPayUtil getAliPayUtil() {
		init();
		return aliPayUtil;
	}

	@Override
	public WechatPayUtil getWechatPayUtil() {
		init();
		return wechatPayUtil;
	}

	@Override
	public String createOrder(long uid, Group group, int month, PayPlatform payPlatform, String returnUrl, HttpServletRequest request) {
		init();
		// 创建订单
		Order order = new Order();
		String tradeNo = PaymentUtil.generateTradeNumber();
		order.setOutTradeNo(tradeNo);
		order.setUid(uid);
		String subject = group.getName() + " " + month + "m";
		order.setSubject(subject);
		order.setPayPlatform(payPlatform.getCode());
		int money = group.getPriceByMonth(month);
		String iMoney = PaymentUtil.changeF2Y(money);
		order.setTotalAmount(money);
		// 插入记录并判断是否插入成功
		boolean saveResult = orderMapper.insert(order) > 0;
		if (!saveResult) {
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "创建订单失败，数据库错误");
		}
		// 发起支付
		try {
			switch (payPlatform) {
				case ALIPAY:
					AlipayTradePrecreateResponse r1 = Factory.Payment.FaceToFace().preCreate(subject, tradeNo, iMoney);
					// 3. 处理响应或异常
					if (ResponseChecker.success(r1)) {
						return r1.getQrCode();
					}
				case ALIPAY_REDIRECT:
					AlipayTradePagePayResponse r2 = Factory.Payment.Page().pay(subject, tradeNo, iMoney, returnUrl);
					if (ResponseChecker.success(r2)) {
						return r2.getBody();
					}
				case WECHAT:
					NativePayService service = new NativePayService.Builder().config(wechatPayUtil.getConfig()).build();
					PrepayRequest r3 = new PrepayRequest();
					Amount amount = new Amount();
					amount.setTotal(money);
					r3.setAmount(amount);
					r3.setAppid(wechatAppId);
					r3.setMchid(wechatMerchantId);
					r3.setDescription(subject);
					String notifyUrl = domain + "/pay/wechat";
					r3.setNotifyUrl(notifyUrl);
					r3.setOutTradeNo(tradeNo);
					// 调用下单方法，得到应答
					PrepayResponse response = service.prepay(r3);
					// 使用微信扫描 code_url 对应的二维码，即可体验Native支付
					return response.getCodeUrl();
				default:
					throw new BusinessException(StatusCode.BAD_REQUEST, "支付平台错误");
			}
		} catch (Exception e) {
			log.error("调用遭遇异常，原因：" + e.getMessage());
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "调用遭遇异常");
		}
	}
}
