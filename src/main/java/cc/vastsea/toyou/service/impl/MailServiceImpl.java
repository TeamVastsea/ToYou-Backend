package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.service.MailService;
import cc.vastsea.toyou.service.UserService;
import jakarta.annotation.Resource;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailServiceImpl implements MailService {
	@Resource
	private UserService userService;
	@Resource
	private JavaMailSender javaMailSender;
	@Value("${spring.mail.username:username@gmail.com}")
	private String from;

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
}
