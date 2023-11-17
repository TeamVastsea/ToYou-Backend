package cc.vastsea.toyou.controller;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.model.dto.EmailCodeGetResponse;
import cc.vastsea.toyou.model.dto.UserCreateRequest;
import cc.vastsea.toyou.model.dto.UserLoginRequest;
import cc.vastsea.toyou.model.entity.Permission;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.model.enums.Group;
import cc.vastsea.toyou.model.vo.UserVO;
import cc.vastsea.toyou.service.MailService;
import cc.vastsea.toyou.service.PermissionService;
import cc.vastsea.toyou.service.UserPictureService;
import cc.vastsea.toyou.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

	@GetMapping("/email/{email}")
	public ResponseEntity<String> getEmailCode(@PathVariable("email") String email, HttpServletRequest request) {
		EmailCodeGetResponse ecr = userService.getEmailCode(email, request);
		if (ecr.isExist()) {
			return new ResponseEntity<>("exists", null, StatusCode.OK);
		}
		mailService.verifyEmail(email, ecr.getCode());
		return new ResponseEntity<>("success", null, StatusCode.CREATED);
	}

	@GetMapping("/{uid}")
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
	public ResponseEntity<UserVO> userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
		UserVO userVO = userService.userLogin(userLoginRequest, request);

		// null
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
}
