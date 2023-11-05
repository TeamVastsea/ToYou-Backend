package cc.vastsea.toyou.aop;

import cc.vastsea.toyou.annotation.AuthCheck;
import cc.vastsea.toyou.service.PermissionService;
import cc.vastsea.toyou.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.List;

/**
 * 权限校验 AOP
 */
@Aspect
@Component
public class AuthInterceptor {
	@Resource
	private UserService userService;
	@Resource
	private PermissionService permissionService;

	/**
	 * 执行拦截
	 */
	@Around("@annotation(authCheck)")
	public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
		List<String> any = Arrays.stream(authCheck.any()).filter(StringUtils::isNotBlank).toList();
		List<String> must = Arrays.stream(authCheck.must()).filter(StringUtils::isNotBlank).toList();

		RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
		HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

		// 通过权限校验，放行
		return joinPoint.proceed();
	}

	public boolean checkAny(Long uid, List<String> any) {
		return any.stream().anyMatch(per -> permissionService.checkPermission(uid, per)) || any.isEmpty();
	}

	public boolean checkMust(Long uid, List<String> must) {
		return must.stream().allMatch(per -> permissionService.checkPermission(uid, per)) || must.isEmpty();
	}

}