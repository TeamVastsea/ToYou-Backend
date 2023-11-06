package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.service.MailService;
import cc.vastsea.toyou.service.UserService;
import cc.vastsea.toyou.util.CaffeineFactory;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class MailServiceImpl implements MailService {
	@Resource
	private UserService userService;
	@Resource
	private JavaMailSender javaMailSender;
	@Resource
	private TemplateEngine templateEngine;
	@Value("${spring.mail.username:username@gmail.com}")
	private String from;
	public static final Cache<String, Boolean> emailSent = CaffeineFactory.newBuilder()
			.expireAfterWrite(1, TimeUnit.MINUTES)
			.build();

	@Async
	@Override
	public void verifyEmail(String to, String code) {
		checkEmail(to);
		MimeMessage message = javaMailSender.createMimeMessage();
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setFrom(from);
			helper.setTo(to);
			helper.setSubject("图邮-邮箱验证码");
			helper.setCc(from);
			Context context = new Context();
			context.setVariable("code", code);
			String text = templateEngine.process("EmailCode", context);
			helper.setText(text, true);
			javaMailSender.send(message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void checkEmail(String to){
		Boolean sent = emailSent.getIfPresent(to);
		if (sent != null){
			throw new BusinessException(500, "邮箱发送过于频繁");
		}
	}
}
