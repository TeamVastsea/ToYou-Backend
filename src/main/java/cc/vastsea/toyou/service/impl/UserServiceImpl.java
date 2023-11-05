package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.common.ErrorCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.mapper.UserMapper;
import cc.vastsea.toyou.model.dto.EmailCodeGetResponse;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.service.UserService;
import cc.vastsea.toyou.util.CaffeineFactory;
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


	public boolean checkDuplicates(String emailRaw) {
		try {
			QueryWrapper<User> queryWrapper = new QueryWrapper<>();
			queryWrapper.eq("emailRaw", emailRaw);
			long count = userMapper.selectCount(queryWrapper);
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
