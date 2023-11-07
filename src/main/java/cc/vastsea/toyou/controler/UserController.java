package cc.vastsea.toyou.controler;

import cc.vastsea.toyou.annotation.AuthCheck;
import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.model.dto.EmailCodeGetResponse;
import cc.vastsea.toyou.model.dto.UserCreateRequest;
import cc.vastsea.toyou.model.dto.UserLoginRequest;
import cc.vastsea.toyou.model.dto.UserLoginResponse;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.model.vo.UserVO;
import cc.vastsea.toyou.service.MailService;
import cc.vastsea.toyou.service.UserService;
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
            return new ResponseEntity<>("exists", null, StatusCode.OK);
		}
		mailService.verifyEmail(email, ecr.getCode());
		return new ResponseEntity<>("created", null, StatusCode.CREATED);
	}

	@GetMapping("/{uid}")
	@AuthCheck(any = "user.get")
	public ResponseEntity<UserVO> getUser(@PathVariable("uid") Long uid, HttpServletRequest request) {
		User user = userService.getUserByUid(uid, request);
		UserVO userVO = new UserVO();
		BeanUtils.copyProperties(user, userVO);
		return new ResponseEntity<>(userVO, null, StatusCode.OK);
	}

	@PostMapping("")
	public ResponseEntity<String> createUser(UserCreateRequest userCreateRequest, HttpServletRequest request) {
		userService.createUser(userCreateRequest, request);
		return new ResponseEntity<>(null, null, StatusCode.CREATED);
	}

	@GetMapping("")
	public ResponseEntity<UUID> userLogin(UserLoginRequest userLoginRequest, @RequestHeader(name = USER_TOKEN_HEADER, required = false) String token, HttpServletRequest request) {
		userLoginRequest.setToken(token);
		UserLoginResponse ulr = userService.userLogin(userLoginRequest, request);
		return new ResponseEntity<>(ulr.getToken(), null, StatusCode.OK);
	}

	@GetMapping("/getlogin")
	public ResponseEntity<UserVO> getLoginUser(HttpServletRequest request) {
		User user = userService.getLoginUser(request);
		UserVO userVO = new UserVO();
		BeanUtils.copyProperties(user, userVO);
		return new ResponseEntity<>(userVO, null, StatusCode.OK);
	}
}
