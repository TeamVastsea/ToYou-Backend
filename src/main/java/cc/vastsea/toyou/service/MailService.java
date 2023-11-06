package cc.vastsea.toyou.service;

import org.springframework.scheduling.annotation.Async;

public interface MailService {
	@Async
	void verifyEmail(String to, String code);
}
