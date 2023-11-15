package cc.vastsea.toyou.controler;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.model.dto.OrderCreationRequest;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.model.enums.Group;
import cc.vastsea.toyou.model.enums.pay.PayPlatform;
import cc.vastsea.toyou.service.OrderService;
import cc.vastsea.toyou.service.PayService;
import cc.vastsea.toyou.service.UserService;
import com.alipay.easysdk.factory.Factory;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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

	@NotNull
	private static Map<String, String> getParamsMap(HttpServletRequest request) {
		Map<String, String> params = new HashMap<>();
		Map<String, String[]> requestParams = request.getParameterMap();
		for (String name : requestParams.keySet()) {
			String[] values = requestParams.get(name);
			String valueStr = "";
			for (int i = 0; i < values.length; i++) {
				valueStr = (i == values.length - 1) ? valueStr + values[i]
						: valueStr + values[i] + ",";
			}
			valueStr = new String(valueStr.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
			params.put(name, valueStr);
		}
		return params;
	}

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

		return new ResponseEntity<>(orderCreationRequest.getGroup().getName() + orderCreationRequest.getPayPlatform().getDesc(), null, StatusCode.OK);
	}

	@PostMapping("/aliPay")
	public String aliPayCallback(HttpServletRequest request) {
		Map<String, String> params = getParamsMap(request);
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
				// 订单金额
				String total_amount = new String(request.getParameter("total_amount").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
				// 实收金额
				String receipt_amount = new String(request.getParameter("receipt_amount").getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
				return "success";
			} else {
				throw new BusinessException(StatusCode.BAD_REQUEST, "验签失败");
			}
		} catch (Exception e) {
			throw new BusinessException(StatusCode.BAD_REQUEST, "验签失败");
		}
	}
}
