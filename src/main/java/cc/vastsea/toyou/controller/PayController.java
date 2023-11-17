package cc.vastsea.toyou.controller;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.model.dto.OrderCreationRequest;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.model.enums.Group;
import cc.vastsea.toyou.model.enums.pay.PayPlatform;
import cc.vastsea.toyou.model.enums.pay.TradeStatus;
import cc.vastsea.toyou.service.OrderService;
import cc.vastsea.toyou.service.PayService;
import cc.vastsea.toyou.service.UserService;
import cc.vastsea.toyou.util.pay.AliPayUtil;
import cc.vastsea.toyou.util.pay.PaymentUtil;
import cc.vastsea.toyou.util.pay.WechatPayUtil;
import com.alipay.easysdk.factory.Factory;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/pay")
@Slf4j
public class PayController {
	@Resource
	private PayService payService;
	@Resource
	private UserService userService;
	@Resource
	private OrderService orderService;

	@GetMapping("")
	public ResponseEntity<String> createOrder(OrderCreationRequest orderCreationRequest, HttpServletRequest request) {
		User user = userService.getTokenLogin(request);
		long uid = user.getUid();
		// 判断目标组的金额是否可以被支付。
		Group group = orderCreationRequest.getGroup();
		if (group == null) {
			throw new BusinessException(StatusCode.BAD_REQUEST, "组不存在");
		}
		if (group.getPrice() <= 0) {
			throw new BusinessException(StatusCode.BAD_REQUEST, "该组不可支付");
		}
		PayPlatform payPlatform = orderCreationRequest.getPayPlatform();
		if (payPlatform == null) {
			throw new BusinessException(StatusCode.BAD_REQUEST, "支付平台错误");
		}
		// 创建订单并发起支付
		String body = payService.createOrder(uid, group, orderCreationRequest.getMonth(), payPlatform, orderCreationRequest.getReturnUrl(), request);
		return new ResponseEntity<>(body, null, StatusCode.OK);
	}

	@PostMapping("/alipay")
	public ResponseEntity<String> aliPayCallback(HttpServletRequest request) {
		// 得到并遍历回调传来的参数
		Map<String, String[]> requestParams = request.getParameterMap();
		Map<String, String> params = new HashMap<>();
		// 将前台的参数转换为"xxx"->"aaa,bbb"的格式存入params中,实际上回调传来的参数每个key都只对应一个value
		for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
			String key = entry.getKey();
			String[] values = entry.getValue();
			StringBuilder valStr = new StringBuilder();
			for (int i = 0; i < values.length; i++) {
				if (i != values.length - 1) {
					valStr.append(values[i]).append(",");
				} else {
					valStr.append(values[i]);
				}
			}
			params.put(key, valStr.toString());
		}
		// 日志打印回调信息，包括签名，支付状态，所有参数
		log.info("alipay_callback, sign:{}, trade_status:{}, params:{}", params.get("sign"), params.get("trade_status"), params);

		// 需要除去sign、sign_type两个参数，而sign已经在#rsaCheckV2方法中除去了
		params.remove("sign_type");
		// 验签
		try {
			boolean signVerified = Factory.Payment.Common().verifyNotify(params);
			if (signVerified) {
				// 商户订单号
				String out_trade_no = new String(request.getParameter("out_trade_no").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
				// 买家支付宝账号
				String buyer_id = new String(request.getParameter("buyer_id").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
				// 支付宝交易号
				String trade_no = new String(request.getParameter("trade_no").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
				// 订单金额(单位:元)
				String total_amount = new String(request.getParameter("total_amount").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
				int total_amount_fen = PaymentUtil.changeY2F(total_amount);
				// 实收金额(单位:元)
				String receipt_amount = new String(request.getParameter("receipt_amount").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
				int receipt_amount_fen = PaymentUtil.changeY2F(receipt_amount);
				// 交易状态
				String trade_status = new String(request.getParameter("trade_status").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
				// 转换
				TradeStatus tradeStatus = AliPayUtil.getTradeStatus(trade_status);
				orderService.callback(out_trade_no, trade_no, total_amount_fen, receipt_amount_fen, tradeStatus);
				return new ResponseEntity<>("success", null, StatusCode.OK);
			} else {
				throw new BusinessException(StatusCode.BAD_REQUEST, "验签失败");
			}
		} catch (Exception e) {
			throw new BusinessException(StatusCode.BAD_REQUEST, "验签失败");
		}
	}

	@PostMapping("/wechat")
	public ResponseEntity<String> wechatCallback(String body, HttpServletRequest request) {
		/*
		  HTTP 请求体 body。切记使用原始报文，不要用 JSON 对象序列化后的字符串，避免验签的 body 和原文不一致。
		  HTTP 头 Wechatpay-Signature。应答的微信支付签名。
		  HTTP 头 Wechatpay-Serial。微信支付平台证书的序列号，验签必须使用序列号对应的微信支付平台证书。
		  HTTP 头 Wechatpay-Nonce。签名中的随机数。
		  HTTP 头 Wechatpay-Timestamp。签名中的时间戳。
		  HTTP 头 Wechatpay-Signature-Type。签名类型。
		 */
		String wechatSignature = request.getHeader("Wechatpay-Signature");
		String wechatPaySerial = request.getHeader("Wechatpay-Serial");
		String wechatpayNonce = request.getHeader("Wechatpay-Nonce");
		String wechatTimestamp = request.getHeader("Wechatpay-Timestamp");
		String wechatSignatureType = request.getHeader("Wechatpay-Signature-Type");
		// 构造 RequestParam
		RequestParam requestParam = new RequestParam.Builder()
				.serialNumber(wechatPaySerial)
				.nonce(wechatpayNonce)
				.signature(wechatSignature)
				.timestamp(wechatTimestamp)
				.body(body)
				.build();
		WechatPayUtil wechatPayUtil = payService.getWechatPayUtil();
		NotificationConfig config = wechatPayUtil.getConfig();

		// 初始化 NotificationParser
		NotificationParser parser = new NotificationParser(config);
		try {
			// 以支付通知回调为例，验签、解密并转换成 Transaction
			Transaction transaction = parser.parse(requestParam, Transaction.class);
			// 商户订单号
			String out_trade_no = transaction.getOutTradeNo();
			// 买家微信信息
			String buyer_id = transaction.getPayer().getOpenid();
			// 微信交易号
			String trade_no = transaction.getTransactionId();
			// 订单金额(单位:分)
			int total_amount = transaction.getAmount().getTotal();
			// 实收金额(单位:分)
			int receipt_amount = transaction.getAmount().getPayerTotal();
			// 交易状态
			Transaction.TradeStateEnum tradeStateEnum = transaction.getTradeState();
			// 转换
			TradeStatus tradeStatus = WechatPayUtil.getTradeStatus(tradeStateEnum);
			orderService.callback(out_trade_no, trade_no, total_amount, receipt_amount, tradeStatus);
		} catch (ValidationException e) {
			// 签名验证失败，返回 401 UNAUTHORIZED 状态码
			log.error("sign verification failed", e);
			return new ResponseEntity<>("sign verification failed", null, StatusCode.UNAUTHORIZED);
		}
		return new ResponseEntity<>("success", null, StatusCode.OK);
	}
}
