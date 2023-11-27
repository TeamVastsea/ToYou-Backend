package cc.vastsea.toyou.service;

import com.aliyuncs.dysmsapi.model.v20170525.SendSmsResponse;

public interface AliyunSmsService {
	SendSmsResponse sendSms(String phoneNumber, String templateParam);
}
