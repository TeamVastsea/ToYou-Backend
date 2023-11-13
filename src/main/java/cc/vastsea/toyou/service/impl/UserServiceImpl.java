package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.mapper.UserMapper;
import cc.vastsea.toyou.model.dto.EmailCodeGetResponse;
import cc.vastsea.toyou.model.dto.UserCreateRequest;
import cc.vastsea.toyou.model.dto.UserLoginRequest;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.model.vo.UserVO;
import cc.vastsea.toyou.service.UserService;
import cc.vastsea.toyou.util.CaffeineFactory;
import cc.vastsea.toyou.util.PasswordUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static cc.vastsea.toyou.constant.UserConstant.*;

@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
	public static final Cache<UUID, Long> userLoginToken = CaffeineFactory.newBuilder()
			.expireAfterWrite(30, TimeUnit.DAYS)
			.build();
	private static final Cache<String, String> emailAuthCode = CaffeineFactory.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build();
	@Resource
	private UserMapper userMapper;

	@Override
	public UserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request) {
		User user;
		String tokenString = request.getHeader(USER_TOKEN_HEADER);
		if (tokenString != null) {//token
			UUID token = UUID.fromString(tokenString);
			user = tokenLogin(token);
			userLoginToken.invalidate(token);
		} else {
			String email = userLoginRequest.getEmail();
			String password = userLoginRequest.getPassword();
			// 检查邮箱格式
			if (!email.matches("^[a-zA-Z0-9._+-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
				throw new BusinessException(StatusCode.BAD_REQUEST, "邮箱格式错误");
			}
			// 检查邮箱是否存在
			String rawEmail = getRawEmail(email);
			QueryWrapper<User> queryWrapper = new QueryWrapper<>();
			queryWrapper.eq("emailRaw", rawEmail);
			user = userMapper.selectOne(queryWrapper);
			if (user == null) {
				throw new BusinessException(StatusCode.UNAUTHORIZED, "邮箱不存在");
			}
			// 检查密码
			if (!PasswordUtil.checkPassword(password, user.getPassword())) {
				throw new BusinessException(StatusCode.UNAUTHORIZED, "密码错误");
			}
		}
		//登录成功
		request.getSession().setAttribute(USER_LOGIN_STATE, user);

		return user.toUserVO();
	}


	/**
	 * 获取当前登录用户
	 */
	@Override
	public User getLoginUser(HttpServletRequest request) {
		// 先判断是否已登录
		Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
		User currentUser = (User) userObj;
		if (currentUser == null || currentUser.getUid() == null) {
			throw new BusinessException(StatusCode.UNAUTHORIZED, "未登录");
		}
		// 从数据库查询（追求性能的话可以注释，直接走缓存）
		long uid = currentUser.getUid();
		String oldPass = currentUser.getPassword();
		currentUser = this.getById(uid);
		if (currentUser == null || !currentUser.getPassword().equals(oldPass)) {
			throw new BusinessException(StatusCode.UNAUTHORIZED, "未登录");
		}

		request.getSession().setAttribute(USER_LOGIN_STATE, currentUser);
		return currentUser;
	}

	@Override
	public void createUser(UserCreateRequest userCreateRequest, HttpServletRequest request) {
		String rawEmail = getRawEmail(userCreateRequest.getEmail());
		String code = emailAuthCode.getIfPresent(rawEmail);
		if (code == null) {
			throw new BusinessException(StatusCode.BAD_REQUEST, "验证码错误");
		}
		if (!code.equals(userCreateRequest.getCode())) {
			throw new BusinessException(StatusCode.UNAUTHORIZED, "验证码错误");
		}
		if (checkDuplicates(rawEmail)) {
			throw new BusinessException(StatusCode.UNAUTHORIZED, "邮箱已被注册");
		}

		// 检查用户名，特殊符号只能使用-和_，其它不能使用。并且检查字符串个数，大于4小于16
		if (!userCreateRequest.getUsername().matches("^[a-zA-Z0-9_-]{4,16}$")) {
			throw new BusinessException(StatusCode.BAD_REQUEST, "用户名格式错误");
		}
		// 检查密码不过分简单。大小写字母、数字、特殊符号中至少包含两个，且长度大于6小于16。
		if (!userCreateRequest.getPassword().matches("^(?![a-zA-Z]+$)(?![A-Z0-9]+$)(?![A-Z\\W_]+$)(?![a-z0-9]+$)(?![a-z\\W_]+$)(?![0-9\\W_]+$)[a-zA-Z0-9\\W_]{6,16}$")) {
			throw new BusinessException(StatusCode.BAD_REQUEST, "密码格式错误");
		}
		User user = new User();
		user.setUsername(userCreateRequest.getUsername());
		user.setPassword(PasswordUtil.encodePassword(userCreateRequest.getPassword()));
		user.setEmail(userCreateRequest.getEmail());
		user.setEmailRaw(rawEmail);
		boolean saveResult = this.save(user);
		if (!saveResult) {
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "添加失败，数据库错误");
		}
		emailAuthCode.invalidate(rawEmail);
	}

	@Override
	public void deleteUser(Long uid, HttpServletRequest request) {
		User user = getUserByUid(uid, request);
		if (user == null) {
			throw new BusinessException(StatusCode.UNAUTHORIZED, "用户不存在");
		}
		this.removeById(uid);
	}

	@Override
	public EmailCodeGetResponse getEmailCode(String email, HttpServletRequest request) {
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

			emailAuthCode.put(email, uppercaseCode);
			emailCodeGetResponse.setExist(false);
			emailCodeGetResponse.setCode(uppercaseCode);

		}
		return emailCodeGetResponse;
	}

	@Override
	public User getUserByUid(Long uid, HttpServletRequest request) {
		if (uid <= 0) {
			throw new BusinessException(StatusCode.BAD_REQUEST, "uid 小于0");
		}
		return this.getById(uid);
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
			throw new BusinessException(StatusCode.BAD_REQUEST, "邮箱格式错误");
		}
		String emailRawName = emailSplit[0].split("\\+")[0];
		return emailRawName + "@" + emailSplit[1];
	}

	@Override
	public String getToken(Long uid) {
		UUID token = UUID.randomUUID();
		userLoginToken.put(token, uid);
		return token.toString();
	}

	@Override
	public User tokenLogin(UUID token) {
		Long uid = userLoginToken.getIfPresent(token);
		if (uid == null) {
			throw new BusinessException(StatusCode.UNAUTHORIZED, "token无效");
		}
		return this.getById(uid);
	}

	@Override
	public void userLogout(HttpServletRequest request) {
		if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
			throw new BusinessException(StatusCode.UNAUTHORIZED, "未登录");
		}
		request.getSession().removeAttribute(USER_LOGIN_STATE);

		String token = request.getHeader("token");
		UUID tokenUUID = UUID.fromString(token);
		userLoginToken.invalidate(tokenUUID);
	}

	@Override
	public User getTokenLogin(HttpServletRequest request) {
		String tokenString = request.getHeader(USER_TOKEN_HEADER);
		if (tokenString == null) {
			return getLoginUser(request);
		}
		UUID token = UUID.fromString(tokenString);
		return tokenLogin(token);
	}
}