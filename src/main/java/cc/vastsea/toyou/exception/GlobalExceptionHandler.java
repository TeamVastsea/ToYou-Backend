package cc.vastsea.toyou.exception;

import cc.vastsea.toyou.common.BaseResponse;
import cc.vastsea.toyou.common.ErrorCode;
import cc.vastsea.toyou.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<String> businessExceptionHandler(BusinessException e) {
		log.error("businessException: " + e.getMessage(), e);
		return new ResponseEntity<>(e.getMessage(), null, e.getCode());
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<String> runtimeExceptionHandler(RuntimeException e) {
		log.error("runtimeException", e);
		return new ResponseEntity<>(e.getMessage(), null, 500);
	}
}
