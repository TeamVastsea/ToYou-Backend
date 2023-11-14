package cc.vastsea.toyou.controler;

import cc.vastsea.toyou.service.PayService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pay")
@Slf4j
public class PayController {
	@Resource
	private PayService payService;

	@GetMapping("/test")
	public void test() {
		payService.test();
	}
}
