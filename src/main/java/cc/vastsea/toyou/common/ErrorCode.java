package cc.vastsea.toyou.common;

import lombok.Getter;

/**
 * 错误码
 */
@Getter
public enum ErrorCode {

	SUCCESS(200),
	BAD_REQUEST(400),
	UNAUTHORIZED(401),
	NOTFOUND(404),
	INTERNAL_SERVER_ERROR(500);


	/**
	 * 状态码
	 */
	private final int code;

	ErrorCode(int code) {
		this.code = code;
	}

}
