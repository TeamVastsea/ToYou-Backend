package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.service.AliyunSmsService;
import cc.vastsea.toyou.util.CaffeineFactory;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest;
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.DefaultProfile;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class AliyunSmsServiceImpl implements AliyunSmsService {
	public static final Cache<String, CodeCache> phoneCache = CaffeineFactory.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build();
	/**
	 * 短信签名名称，这是您在阿里云短信服务中创建的签名的名称。
	 */
	private final String signName = "图邮";
	/**
	 * 阿里云accessKeyId
	 */
	@Value("${sms.aliyun.access-key-id}")
	private String accessKeyId;
	/**
	 * 阿里云accessKeySecret
	 */
	@Value("${sms.aliyun.access-key-secret}")
	private String accessKeySecret;
	/**
	 * 短信模板代码，这是您在阿里云短信服务中创建的模板的代码。
	 */
	@Value("${sms.aliyun.template-code}")
	private String templateCode;

	@Override
	public SendSmsResponse sendSms(String phoneNumber, String templateParam) {
		CodeCache codeCache = phoneCache.getIfPresent(phoneNumber);
		if (codeCache != null) {
			long time = codeCache.getTime();
			// 如果距离上次发送时间小于60秒，则不发送
			if (System.currentTimeMillis() - time < 60 * 1000) {
				throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "短信发送过于频繁");
			}
		}
		DefaultProfile profile = DefaultProfile.getProfile("cn-hangzhou", accessKeyId, accessKeySecret);
		IAcsClient client = new DefaultAcsClient(profile);

		SendSmsRequest request = new SendSmsRequest();
		request.setPhoneNumbers(phoneNumber);
		request.setSignName(signName);
		request.setTemplateCode(templateCode);
		request.setTemplateParam(templateParam);

		CodeCache cache = new CodeCache(templateCode, System.currentTimeMillis());
		phoneCache.put(phoneNumber, cache);
		try {
			return client.getAcsResponse(request);
		} catch (ClientException e) {
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "短信发送失败");
		}
	}

	@Data
	public static class CodeCache {
		@NotNull
		private String code;
		@NotNull
		private Long time;
	}
}
