package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.service.PayService;
import cc.vastsea.toyou.util.pay.AliPayUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PayServiceImpl implements PayService {
	private final AliPayUtil aliPayUtil = new AliPayUtil();
	@Value("${domain:http://localhost:8102}")
	private String domain;

	@Override
	public void test() {
	}
}
