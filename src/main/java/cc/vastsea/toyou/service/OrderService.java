package cc.vastsea.toyou.service;

import cc.vastsea.toyou.model.entity.Order;
import cc.vastsea.toyou.model.enums.pay.TradeStatus;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderService extends IService<Order> {
	void callback(String outTradeNo, String tradeNo, int totalAmount, int receiptAmount, TradeStatus tradeStatus);
}
