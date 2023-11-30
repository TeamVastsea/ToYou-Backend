package cc.vastsea.toyou.service;

import cc.vastsea.toyou.model.dto.CodeGetResponse;
import cc.vastsea.toyou.model.dto.UserCreateRequest;
import cc.vastsea.toyou.model.dto.UserLoginRequest;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.model.vo.UserVO;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

public interface UserService extends IService<User> {
	UserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

	User getLoginUser(HttpServletRequest request);

	void createUser(UserCreateRequest userCreateRequest, HttpServletRequest request);

	void deleteUser(Long uid, HttpServletRequest request);

	CodeGetResponse getCode(String email, Boolean onlyNum);

	User getUserByUid(Long uid, HttpServletRequest request);

	String getToken(Long uid);

	User tokenLogin(UUID token);

	void userLogout(HttpServletRequest request);

	User getTokenLogin(HttpServletRequest request);

	boolean checkDuplicatesPhone(String phone);

	boolean checkDuplicatesEmail(String emailRaw);

	String getRawEmail(String email);
}
