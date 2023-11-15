package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.mapper.OrderMapper;
import cc.vastsea.toyou.model.entity.Order;
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
}
