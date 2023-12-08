package cc.vastsea.toyou.controller;

import cc.vastsea.toyou.annotation.AuthCheck;
import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.service.PictureService;
import cc.vastsea.toyou.service.UserService;
import cc.vastsea.toyou.task.CleanupEndTask;
import cc.vastsea.toyou.util.NetUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {
	@Resource
	private UserService userService;

	@Resource
	private PictureService pictureService;

	@Autowired
	private CleanupEndTask cleanupEndTask;

	@DeleteMapping("/user/{uid}")
	@AuthCheck(must = "*")
	public ResponseEntity<Boolean> deleteUser(@PathVariable("uid") Long uid, HttpServletRequest request) {
		userService.deleteUser(uid, request);
		return new ResponseEntity<>(true, null, StatusCode.OK);
	}



	// 开始清理工作
	@PostMapping("/cleanup")
	public ResponseEntity<Boolean> startCleanup(HttpServletRequest request){
		if(!NetUtil.isLocalAddress(request)) {
			return new ResponseEntity<>(false,null,StatusCode.FORBIDDEN);
		}
		pictureService.setAllowSaveToDatabase(false);
		// 超过10分钟未结束清理工作写入缓存
		cleanupEndTask.scheduleTask();
		return new ResponseEntity<>(true,null,StatusCode.OK);
	}

	// 结束清理
	@DeleteMapping("/cleanup")
	public ResponseEntity<Boolean> endCleanup(HttpServletRequest request){
		if(!NetUtil.isLocalAddress(request)) {
			return new ResponseEntity<>(false, null, StatusCode.FORBIDDEN);
		}
		cleanupEndTask.save();
		return new ResponseEntity<>(true,null,StatusCode.OK);
	}


}
