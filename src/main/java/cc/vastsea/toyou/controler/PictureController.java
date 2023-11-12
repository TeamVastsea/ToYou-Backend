package cc.vastsea.toyou.controler;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.model.entity.Picture;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.model.entity.UserPicture;
import cc.vastsea.toyou.model.enums.Group;
import cc.vastsea.toyou.service.PermissionService;
import cc.vastsea.toyou.service.PictureService;
import cc.vastsea.toyou.service.UserPictureService;
import cc.vastsea.toyou.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {
	@Resource
	private PictureService pictureService;
	@Resource
	private PermissionService permissionService;
	@Resource
	private UserPictureService userPictureService;
	@Resource
	private UserService userService;

	@PostMapping("")
	public ResponseEntity<String> uploadPicture(@NotNull String fileName, @RequestBody MultipartFile file, HttpServletRequest request) {
		User user = userService.getLoginUser(request);
		long uid = user.getUid();
		Group group = permissionService.getMaxPriorityGroup(uid);
		long pictureSize = file.getSize();
		// 获取当前用户已使用的空间
		long usedStorage = userPictureService.getUsedStorage(uid);
		long storageByte = group.getStorageByte();
		// 判断是否可以存储下这张图片
		if (usedStorage + pictureSize > storageByte) {
			throw new BusinessException(StatusCode.FORBIDDEN, "空间不足");
		}
		// 判断是否存在同名的文件
		if (userPictureService.isExistSameName(uid, fileName)) {
			throw new BusinessException(StatusCode.FORBIDDEN, "存在同名文件");
		}
		Picture picture;
		try {
			picture = pictureService.uploadPicture(file).get();
		} catch (Throwable e) {
			log.error("系统异常", e);
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "系统异常");
		}
		userPictureService.addUserPicture(uid, picture.getPid(), fileName);
		usedStorage = userPictureService.getUsedStorage(uid);
		return new ResponseEntity<>(String.valueOf(usedStorage), null, StatusCode.CREATED);
	}


	@DeleteMapping("/{id}")
	public ResponseEntity<String> deletePicture(@PathVariable("id") Long id, HttpServletRequest request) {
		User user = userService.getLoginUser(request);
		long uid = user.getUid();
		UserPicture userPicture = userPictureService.getUserPicture(uid, id);
		if (userPicture == null) {
			throw new BusinessException(StatusCode.FORBIDDEN, "无权操作");
		}
		userPictureService.removeById(id);
		return new ResponseEntity<>("success", null, StatusCode.OK);
	}

	@PatchMapping("/{id}")
	public ResponseEntity<String> updateShareMode(@PathVariable("id") Long id, @NotNull Integer shareMode, HttpServletRequest request) {
		User user = userService.getLoginUser(request);
		long uid = user.getUid();
		UserPicture userPicture = userPictureService.getUserPicture(uid, id);
		if (userPicture == null) {
			throw new BusinessException(StatusCode.FORBIDDEN, "无权操作");
		}
		userPictureService.updateShareMode(userPicture, shareMode);
		return new ResponseEntity<>("success", null, StatusCode.OK);
	}

	@GetMapping("/picture/{id}/meta")
	public ResponseEntity<Picture> getPictureMeta(@PathVariable("id") Long id, HttpServletRequest request) {
		User user = userService.getLoginUser(request);
		long uid = user.getUid();
		UserPicture userPicture = userPictureService.getUserPicture(uid, id);
		if (userPicture == null) {
			throw new BusinessException(StatusCode.FORBIDDEN, "无权操作");
		}
		Picture picture = pictureService.getById(userPicture.getPid());
		return new ResponseEntity<>(picture, null, StatusCode.OK);
	}
}
