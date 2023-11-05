package cc.vastsea.toyou.service;

import cc.vastsea.toyou.model.dto.EmailCodeGetResponse;
import cc.vastsea.toyou.model.dto.UserCreateRequest;
import cc.vastsea.toyou.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
	void createUser(UserCreateRequest userCreateRequest);

	EmailCodeGetResponse getEmailCode(String email);
}
