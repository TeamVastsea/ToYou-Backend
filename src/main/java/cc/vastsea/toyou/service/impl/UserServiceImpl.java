package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.common.ErrorCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.mapper.UserMapper;
import cc.vastsea.toyou.model.dto.EmailCodeGetResponse;
import cc.vastsea.toyou.model.dto.UserCreateRequest;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.service.UserService;
import cc.vastsea.toyou.util.CaffeineFactory;
import cc.vastsea.toyou.util.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
	private static final Cache<String, String> emailAuthCode = CaffeineFactory.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build();
	@Resource
	private UserMapper userMapper;

	@Override
	public void createUser(UserCreateRequest userCreateRequest) {
		String rawEmail = getRawEmail(userCreateRequest.getEmail());
		String code = emailAuthCode.getIfPresent(rawEmail);
		if (code == null) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "请先获取验证码");
		}
		if (!code.equals(userCreateRequest.getCode())) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
		}
		if (checkDuplicates(rawEmail)) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱已被注册");
		}
		// 检查用户名，特殊符号只能使用-和_，其它不能使用。并且检查字符串个数，大于4小于16
		if (!userCreateRequest.getUsername().matches("^[a-zA-Z0-9_-]{4,16}$")) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名格式错误");
		}
		// 检查密码不过分简单。大小写字母、数字、特殊符号中至少包含两个，且长度大于6小于16。
		if (!userCreateRequest.getPassword().matches("^(?![a-zA-Z]+$)(?![A-Z0-9]+$)(?![A-Z\\W_]+$)(?![a-z0-9]+$)(?![a-z\\W_]+$)(?![0-9\\W_]+$)[a-zA-Z0-9\\W_]{6,16}$")) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码格式错误");
		}
		User user = new User();
		user.setUsername(userCreateRequest.getUsername());
		user.setPassword(PasswordUtil.encodePassword(userCreateRequest.getPassword()));
		user.setEmail(userCreateRequest.getEmail());
		user.setEmailRaw(rawEmail);
		this.save(user);
		emailAuthCode.invalidate(rawEmail);
	}

	@Override
	public EmailCodeGetResponse getEmailCode(String email) {
		String rawEmail = getRawEmail(email);

		EmailCodeGetResponse emailCodeGetResponse = new EmailCodeGetResponse();
		if (checkDuplicates(rawEmail)) {
			emailCodeGetResponse.setExist(true);
		} else {
			String codeChars = "123456789QWERTYUIOPASDFGHJKLZXCVBNM";
			StringBuilder code = new StringBuilder();
			Random random = new Random();
			for (int i = 0; i < 6; i++) {
				code.append(codeChars.charAt(random.nextInt(codeChars.length())));
			}
			String uppercaseCode = code.toString().toUpperCase();

			emailAuthCode.put(rawEmail, uppercaseCode);
			emailCodeGetResponse.setExist(false);
			emailCodeGetResponse.setCode(uppercaseCode);
		}
		return emailCodeGetResponse;
	}

	@Override
	public User getUserByUid(Long uid) {
		if (uid <= 0) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR);
		}
		return this.getById(uid);
	}


	public boolean checkDuplicates(String emailRaw) {
		try {
			QueryWrapper<User> queryWrapper = new QueryWrapper<>();
			queryWrapper.eq("emailRaw", emailRaw);
			long count = userMapper.selectCount(queryWrapper);
			log.error(count + "数量");
			return count > 0;
		} catch (Throwable e) {
			log.error(e.toString());
			return true;
		}
	}

	public String getRawEmail(String email) {
		String[] emailSplit = email.split("@");
		if (emailSplit.length != 2) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式错误");
		}
		String emailRawName = emailSplit[0].split("\\+")[0];
		return emailRawName + "@" + emailSplit[1];
	}

}
