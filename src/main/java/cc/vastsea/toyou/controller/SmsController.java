package cc.vastsea.toyou.controller;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.service.AliyunSmsService;
import cc.vastsea.toyou.util.NumberUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
@Slf4j
public class SmsController {
	@Resource
	private AliyunSmsService aliyunSmsService;

	@GetMapping("/{phone}")
	public ResponseEntity<String> sendSms(@PathVariable("phone") String phone) {
		String code = NumberUtil.getRandomCode(6);
		aliyunSmsService.sendSms(phone, code);
		return new ResponseEntity<>("success", null, StatusCode.OK);
	}
}
