package cc.vastsea.toyou.controler;

import cc.vastsea.toyou.annotation.AuthCheck;
import cc.vastsea.toyou.common.BaseResponse;
import cc.vastsea.toyou.common.ResultUtils;
import cc.vastsea.toyou.model.dto.EmailCodeGetResponse;
import cc.vastsea.toyou.model.dto.UserCreateRequest;
import cc.vastsea.toyou.model.dto.UserLoginRequest;
import cc.vastsea.toyou.model.dto.UserLoginResponse;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.model.vo.UserVO;
import cc.vastsea.toyou.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static cc.vastsea.toyou.constant.UserConstant.*;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
	@Resource
	private UserService userService;

	@GetMapping("/email/{email}")
	public BaseResponse<String> getEmailCode(@PathVariable("email") String email, HttpServletRequest request) {
		EmailCodeGetResponse ecr = userService.getEmailCode(email, request);
		// return ResultUtils.success(ecr);
		log.debug("code:" + ecr.getCode());
		return ResultUtils.success("success");
	}

	@GetMapping("/{uid}")
	@AuthCheck(any = "user.get")
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

	@GetMapping("")
	public BaseResponse<UUID> userLogin(UserLoginRequest userLoginRequest, @RequestHeader(name = USER_TOKEN_HEADER, required = false) String token, HttpServletRequest request) {
		userLoginRequest.setToken(token);
		UserLoginResponse ulr = userService.userLogin(userLoginRequest, request);
		return ResultUtils.success(ulr.getToken());
	}

	@GetMapping("/getlogin")
	public BaseResponse<UserVO> getLoginUser(HttpServletRequest request) {
		User user = userService.getLoginUser(request);
		UserVO userVO = new UserVO();
		BeanUtils.copyProperties(user, userVO);
		return ResultUtils.success(userVO);
	}
}
