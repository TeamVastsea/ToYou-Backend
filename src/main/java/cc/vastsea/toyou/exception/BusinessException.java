package cc.vastsea.toyou.exception;


import cc.vastsea.toyou.common.ErrorCode;
import lombok.Getter;

/**
 * 自定义异常类
 */
@Getter
public class BusinessException extends RuntimeException {

	private final int code;

	public BusinessException(int code, String message) {
		super(message);
		this.code = code;
	}

	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.code = errorCode.getCode();
	}

}
