package cc.vastsea.toyou.controler;

import cc.vastsea.toyou.common.BaseResponse;
import cc.vastsea.toyou.common.ResultUtils;
import cc.vastsea.toyou.model.dto.EmailCodeGetResponse;
import cc.vastsea.toyou.model.dto.UserCreateRequest;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.model.vo.UserVO;
import cc.vastsea.toyou.service.MailService;
import cc.vastsea.toyou.service.PermissionService;
import cc.vastsea.toyou.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
	@Resource
	private PermissionService permissionService;

	@GetMapping("/email/{email}")
	public BaseResponse<EmailCodeGetResponse> getEmailCode(@PathVariable("email") String email, HttpServletRequest request) {
		EmailCodeGetResponse ecr = userService.getEmailCode(email, request);
		return ResultUtils.success(ecr);
		// todo return ResultUtils.success("success");
	}

	@GetMapping("/{uid}")
	public BaseResponse<UserVO> getUser(@PathVariable("uid") Long uid, HttpServletRequest request) {
		User user = userService.getUserByUid(uid, request);
		UserVO userVO = new UserVO();
		BeanUtils.copyProperties(user, userVO);
		return ResultUtils.success(userVO);
	}

	@PostMapping("")
	public BaseResponse<String> createUser(UserCreateRequest userCreateRequest, HttpServletRequest request) {
		userService.createUser(userCreateRequest, request);
		return ResultUtils.success("success");
	}
}
