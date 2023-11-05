package cc.vastsea.toyou.controler;

import cc.vastsea.toyou.common.BaseResponse;
import cc.vastsea.toyou.common.ResultUtils;
import cc.vastsea.toyou.model.dto.EmailCodeGetResponse;
import cc.vastsea.toyou.service.MailService;
import cc.vastsea.toyou.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
	@Resource
	private UserService userService;
	@Resource
	private MailService mailService;

	@GetMapping("/{email}")
	public BaseResponse<EmailCodeGetResponse> getEmailCode(@PathVariable("email") String email) {
		EmailCodeGetResponse ecr = userService.getEmailCode(email);
		// mailService.mailTest(email, ecr.getCode());
		return ResultUtils.success(ecr);
	}
}
