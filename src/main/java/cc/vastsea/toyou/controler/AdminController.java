package cc.vastsea.toyou.controler;

import cc.vastsea.toyou.annotation.AuthCheck;
import cc.vastsea.toyou.common.BaseResponse;
import cc.vastsea.toyou.common.ResultUtils;
import cc.vastsea.toyou.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {
	@Resource
	private UserService userService;

	@DeleteMapping("/user/{uid}")
	@AuthCheck(must = "*")
	public BaseResponse<Boolean> deleteUser(@PathVariable("uid") Long uid, HttpServletRequest request) {
		userService.deleteUser(uid, request);
		return ResultUtils.success(true);
	}
}
