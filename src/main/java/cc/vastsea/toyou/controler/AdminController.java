package cc.vastsea.toyou.controler;

import cc.vastsea.toyou.annotation.AuthCheck;
import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {
	@Resource
	private UserService userService;

	@DeleteMapping("/user/{uid}")
	@AuthCheck(must = "*")
	public ResponseEntity<Boolean> deleteUser(@PathVariable("uid") Long uid, HttpServletRequest request) {
		userService.deleteUser(uid, request);
		return new ResponseEntity<>(true, null, StatusCode.OK);
	}
}
