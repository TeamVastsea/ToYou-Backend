package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.mapper.OrderMapper;
import cc.vastsea.toyou.model.entity.Order;
import cc.vastsea.toyou.model.enums.pay.TradeStatus;
import cc.vastsea.toyou.service.OrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
	@Resource
	private OrderMapper orderMapper;

	@Override
	public void callback(String outTradeNo, String tradeNo, int totalAmount, int receiptAmount, TradeStatus tradeStatus) {
		Order order = orderMapper.selectById(outTradeNo);
		if (order == null) {
			throw new BusinessException(StatusCode.BAD_REQUEST, "订单不存在");
		}
		order.setTradeNo(tradeNo);
		order.setReceiptAmount(receiptAmount);
		order.setTradeStatus(tradeStatus.getCode());
		orderMapper.updateById(order);
	}
}
