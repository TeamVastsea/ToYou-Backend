package cc.vastsea.toyou.service;

import cc.vastsea.toyou.model.dto.EmailCodeGetResponse;
import cc.vastsea.toyou.model.dto.UserCreateRequest;
import cc.vastsea.toyou.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService extends IService<User> {
	void createUser(UserCreateRequest userCreateRequest, HttpServletRequest request);

	EmailCodeGetResponse getEmailCode(String email,HttpServletRequest request);

	User getUserByUid(Long uid,HttpServletRequest request);
}
