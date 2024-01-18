package cc.vastsea.toyou.controller;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.mapper.RealNameMapper;
import cc.vastsea.toyou.model.dto.CodeGetResponse;
import cc.vastsea.toyou.model.dto.UserCreateRequest;
import cc.vastsea.toyou.model.dto.UserLoginRequest;
import cc.vastsea.toyou.model.entity.Permission;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.model.enums.Group;
import cc.vastsea.toyou.model.vo.UserVO;
import cc.vastsea.toyou.service.*;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

import static cc.vastsea.toyou.constant.UserConstant.*;

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
	@Resource
	private UserPictureService userPictureService;
	@Resource
	private AliyunSmsService aliyunSmsService;

	@Resource
	private RealNameMapper realNameMapper;

	private static final Pattern numberPattern = Pattern.compile("[0-9]*");

	@GetMapping("/code/email")
	public ResponseEntity<String> getEmailCode(@RequestParam("email") String email) {
		CodeGetResponse ecr = userService.getCode(email, false);
		if (ecr.getFrequent()) {
			return new ResponseEntity<>("too frequent", null, StatusCode.TOO_MANY_REQUESTS);
		}
		mailService.verifyEmail(email, ecr.getCode());
		return new ResponseEntity<>("{\"cd\": 60000}", null, StatusCode.OK);
	}

	@GetMapping("/code/phone")
	public ResponseEntity<String> getPhoneCode(@RequestParam String phone) {
		if (phone.length() != 11 || !numberPattern.matcher(phone).matches()) {
			return new ResponseEntity<>("not a phone number", null, StatusCode.BAD_REQUEST);
		}
		CodeGetResponse ecr = userService.getCode(phone, true);
		if (ecr.getFrequent()) {
			return new ResponseEntity<>("too frequent", null, StatusCode.TOO_MANY_REQUESTS);
		}
		aliyunSmsService.sendSms(phone, "{\"code\":\"" + ecr.getCode() + "\"}");
		return new ResponseEntity<>("{\"cd\": 60000}", null, StatusCode.OK);
	}

	@GetMapping("/email/{email}")
	public ResponseEntity<String> checkEmail(@PathVariable("email") String email) {
		String rawEmail = userService.getRawEmail(email);
		if (userService.checkDuplicatesEmail(rawEmail)) {
			return new ResponseEntity<>("true", null, StatusCode.OK);
		}
		return new ResponseEntity<>("false", null, StatusCode.OK);
	}

	@GetMapping("/phone/{phone}")
	public ResponseEntity<String> checkPhone(@PathVariable("phone") String phone) {
		if (phone.length() != 11 || !numberPattern.matcher(phone).matches()) {
			return new ResponseEntity<>("not a phone", null, StatusCode.BAD_REQUEST);
		}

		if (userService.checkDuplicatesPhone(phone)) {
			return new ResponseEntity<>("true", null, StatusCode.OK);
		}
		return new ResponseEntity<>("false", null, StatusCode.OK);
	}

	@GetMapping("/uid/{uid}")
	public ResponseEntity<UserVO> getUser(@PathVariable("uid") Long uid, HttpServletRequest request) {
		User user = userService.getUserByUid(uid, request);
		UserVO userVO = new UserVO();
		BeanUtils.copyProperties(user, userVO);
		return new ResponseEntity<>(userVO, null, StatusCode.OK);
	}

	@PostMapping("")
	public ResponseEntity<String> createUser(UserCreateRequest userCreateRequest, HttpServletRequest request) {
		userService.createUser(userCreateRequest, request);
		return new ResponseEntity<>("success", null, StatusCode.CREATED);
	}

	@GetMapping("/name/verify")
	public ResponseEntity<Boolean> certify(HttpServletRequest httpServletRequest){

		User tokenLogin = userService.getTokenLogin(httpServletRequest);
		if(tokenLogin==null) return new ResponseEntity<>(false,null,StatusCode.OK);

		return new ResponseEntity<>(realNameMapper.selectById(tokenLogin.getUid())!=null,null,StatusCode.OK);
	}

	@GetMapping("")
	public ResponseEntity<UserVO> userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
		UserVO userVO = userService.userLogin(userLoginRequest, request);

		Boolean ext = userLoginRequest.getExtended();
		if (ext != null && ext) {
			UserVO.ExtendUserInformation extended = new UserVO.ExtendUserInformation();
			Permission gp = permissionService.getMaxPriorityGroupP(userVO.getUid());
			long usedStorage = userPictureService.getUsedStorage(userVO.getUid());

			extended.setStorageUsed(usedStorage);
			extended.setUserGroup(gp == null ? Group.DEFAULT : gp.getGroup());
			extended.setGroupEndDate(gp == null ? 0 : gp.getExpiry());
			extended.setGroupStartDate(gp == null ? null : gp.getGroup() == Group.DEFAULT ? null : gp.getCreateTime().getTime());
			extended.setGroupUpdateDate(gp == null ? null : gp.getGroup() == Group.DEFAULT ? null : gp.getUpdateTime().getTime());
			userVO.setExtend(extended);
		}

		MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
		if (request.getHeader(USER_TOKEN_HEADER) == null) {
			headers.add(USER_TOKEN_HEADER, userService.getToken(userVO.getUid()));
		}

		return new ResponseEntity<>(userVO, headers, StatusCode.OK);
	}

	@DeleteMapping("/token")
	public ResponseEntity<String> userLogout(HttpServletRequest request) {
		userService.userLogout(request);
		return new ResponseEntity<>("success", null, StatusCode.OK);
	}

	@PatchMapping("/username")
	public ResponseEntity<String> changeUsername(@RequestParam("username") String newUsername, HttpServletRequest request) {
		User user = userService.getTokenLogin(request);
		userService.changeUsername(user, newUsername);
		return new ResponseEntity<>("", null, StatusCode.OK);
	}

	@PatchMapping("/password")
	public ResponseEntity<String> changePassword(@RequestParam("old") String oldPassword, @RequestParam("new") String newPassword, HttpServletRequest request) {
		User user = userService.getTokenLogin(request);
		userService.changePassword(user, oldPassword, newPassword);
		return new ResponseEntity<>("", null, StatusCode.OK);
	}
}
