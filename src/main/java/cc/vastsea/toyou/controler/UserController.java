package cc.vastsea.toyou.controler;

import cc.vastsea.toyou.annotation.AuthCheck;
import cc.vastsea.toyou.common.BaseResponse;
import cc.vastsea.toyou.common.ErrorCode;
import cc.vastsea.toyou.common.ResultUtils;
import cc.vastsea.toyou.model.dto.EmailCodeGetResponse;
import cc.vastsea.toyou.model.dto.UserCreateRequest;
import cc.vastsea.toyou.model.dto.UserLoginRequest;
import cc.vastsea.toyou.model.dto.UserLoginResponse;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.model.vo.UserVO;
import cc.vastsea.toyou.service.MailService;
import cc.vastsea.toyou.service.UserService;
import cc.vastsea.toyou.service.impl.MailServiceImpl;
import cc.vastsea.toyou.util.CaffeineFactory;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
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
	@Resource
	private MailService mailService ;

	@GetMapping("/email/{email}")
	public ResponseEntity<String> getEmailCode(@PathVariable("email") String email, HttpServletRequest request) {
		EmailCodeGetResponse ecr = userService.getEmailCode(email, request);
		if (ecr.isExist()) {
            return new ResponseEntity<>("exists", null, 200);
		}
		mailService.verifyEmail(email, ecr.getCode());
		return new ResponseEntity<>("created", null, 200);
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
	public ResponseEntity<String> createUser(UserCreateRequest userCreateRequest, HttpServletRequest request) {
		userService.createUser(userCreateRequest, request);
		return new ResponseEntity<>(null, null, 201);
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
