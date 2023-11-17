package cc.vastsea.toyou.controller;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.model.dto.PicturePreviewRequest;
import cc.vastsea.toyou.model.dto.SharePictureRequest;
import cc.vastsea.toyou.model.dto.UserPictureListRequest;
import cc.vastsea.toyou.model.entity.Picture;
import cc.vastsea.toyou.model.entity.Share;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.model.entity.UserPicture;
import cc.vastsea.toyou.model.enums.Group;
import cc.vastsea.toyou.model.enums.ShareMode;
import cc.vastsea.toyou.model.vo.UserPictureVO;
import cc.vastsea.toyou.service.PermissionService;
import cc.vastsea.toyou.service.PictureService;
import cc.vastsea.toyou.service.ShareService;
import cc.vastsea.toyou.service.UserPictureService;
import cc.vastsea.toyou.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
	@Resource
	private ShareService shareService;

	@PostMapping("")
	public ResponseEntity<String> uploadPicture(@NotNull String fileName, @RequestBody MultipartFile file, HttpServletRequest request) {
		User user = userService.getTokenLogin(request);
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
		User user = userService.getTokenLogin(request);
		long uid = user.getUid();
		UserPicture userPicture = userPictureService.getUserPicture(uid, id);
		if (userPicture == null) {
			throw new BusinessException(StatusCode.FORBIDDEN, "无权操作");
		}
		userPictureService.removeById(id);
		return new ResponseEntity<>("success", null, StatusCode.OK);
	}

	@GetMapping("/{id}/meta")
	public ResponseEntity<UserPictureVO> getPictureMeta(@PathVariable("id") Long id, HttpServletRequest request) {
		User user = userService.getTokenLogin(request);
		long uid = user.getUid();
		UserPicture userPicture = userPictureService.getUserPicture(uid, id);
		if (userPicture == null) {
			throw new BusinessException(StatusCode.FORBIDDEN, "无权操作");
		}
		long pictureSize = pictureService.getPicture(userPicture.getPid()).getSize();

		UserPictureVO userPictureVO = new UserPictureVO();
		BeanUtils.copyProperties(userPicture, userPictureVO);
		userPictureVO.setSize(pictureSize);
		return new ResponseEntity<>(userPictureVO, null, StatusCode.OK);
	}

	@GetMapping("")
	public ResponseEntity<Page<UserPictureVO>> getUserPictures(UserPictureListRequest userPictureListRequest, HttpServletRequest request) {
		if (userPictureListRequest == null) {
			throw new BusinessException(StatusCode.FORBIDDEN, "参数错误");
		}
		User user = userService.getTokenLogin(request);
		long uid = user.getUid();
		userPictureListRequest.setUid(uid);

		UserPicture userPictureQuery = new UserPicture();
		BeanUtils.copyProperties(userPictureListRequest, userPictureQuery);
		long current = userPictureListRequest.getCurrent();
		long size = userPictureListRequest.getPageSize();

		// 限制爬虫
		if (size > 100) {
			throw new BusinessException(StatusCode.FORBIDDEN, "参数错误");
		}

		QueryWrapper<UserPicture> queryWrapper = new QueryWrapper<>(userPictureQuery);
		Page<UserPicture> userPicturePage = userPictureService.page(new Page<>(current, size), queryWrapper);
		Page<UserPictureVO> userPictureVOPage = new PageDTO<>(userPicturePage.getCurrent(), userPicturePage.getSize(), userPicturePage.getTotal());
		List<UserPictureVO> userPictureVOList = userPicturePage.getRecords().stream().map(userPicture -> {
			UserPictureVO userPictureVO = new UserPictureVO();
			BeanUtils.copyProperties(userPicture, userPictureVO);
			Picture picture = pictureService.getPicture(userPicture.getPid());
			long fileSize = picture.getSize();
			userPictureVO.setSize(fileSize);
			return userPictureVO;
		}).collect(Collectors.toList());
		userPictureVOPage.setRecords(userPictureVOList);
		return new ResponseEntity<>(userPictureVOPage, null, StatusCode.OK);
	}

	@PostMapping("/share/{id}")
	public ResponseEntity<Share> sharePicture(SharePictureRequest sharePictureRequest, @PathVariable("id") Long id, HttpServletRequest request) {
		User user = userService.getTokenLogin(request);
		long uid = user.getUid();
		UserPicture userPicture = userPictureService.getUserPicture(uid, id);
		if (userPicture == null) {
			throw new BusinessException(StatusCode.FORBIDDEN, "无权操作");
		}
		Share share = shareService.addShare(userPicture, sharePictureRequest);
		return new ResponseEntity<>(share, null, StatusCode.OK);
	}

	@GetMapping("/share/{uuid}")
	public ResponseEntity<String> downloadPicture(String password, @PathVariable("uuid") String uuid, HttpServletRequest request, HttpServletResponse response) {
		Share share = shareService.getShare(uuid);
		if (!StringUtils.isAnyBlank(share.getPassword())) {
			if (!share.getPassword().equals(password)) {
				throw new BusinessException(StatusCode.FORBIDDEN, "密码错误");
			}
		}
		long id = share.getId();
		UserPicture userPicture = userPictureService.getById(id);
		if (userPicture == null) {
			throw new BusinessException(StatusCode.FORBIDDEN, "未找到该图片");
		}
		String pid = userPicture.getPid();
		String fileName = userPicture.getFileName();
		ShareMode shareMode = ShareMode.of(share.getShareMode());
		if (shareMode == ShareMode.PRIVATE) {
			throw new BusinessException(StatusCode.FORBIDDEN, "无权操作");
		}
		Picture picture = pictureService.getById(pid);
		File pictureFile;
		switch (shareMode) {
			case PUBLIC -> pictureFile = new File(picture.getWatermark());
			case PUBLIC_COMPRESS -> pictureFile = new File(picture.getThumbnail());
			case PUBLIC_ORIGINAL -> pictureFile = new File(picture.getOriginal());
			default -> throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "文件系统异常");
		}
		try {
			InputStream is = new FileInputStream(pictureFile);
			//创建字节数组，获取当前文件中所有的字节数
			byte[] bytes = new byte[is.available()];
			//将流读到字节数组中
			is.read(bytes);
			//设置响应头信息，Content-Disposition响应头表示收到的数据怎么处理（固定），attachment表示下载使用（固定），filename指定下载的文件名（下载时会在客户端显示该名字）
			response.addHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + fileName);
			//创建输出流
			OutputStream out = response.getOutputStream();
			out.write(bytes);
			//关闭资源
			is.close();
			out.flush();
		} catch (Throwable e) {
			log.error("下载系统异常", e);
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "下载系统异常");
		}
		// 增加下载次数
		userPictureService.setDownloads(id, userPicture.getDownloads() + 1);
		shareService.setDownloads(uuid, share.getDownloads() + 1);
		return new ResponseEntity<>("success", null, StatusCode.OK);
	}

	@GetMapping("/preview")
	public ResponseEntity<InputStreamResource> previewPicture(PicturePreviewRequest picturePreviewRequest, HttpServletRequest request, HttpServletResponse response) {
		User user = null;
		if (picturePreviewRequest.getToken() != null) {
			user = userService.tokenLogin(UUID.fromString(picturePreviewRequest.getToken()));
		} else {
			user = userService.getTokenLogin(request);
		}

		if (user == null) {
			throw new BusinessException(StatusCode.BAD_REQUEST, "token失效");
		}

		long uid = user.getUid();
		long id = picturePreviewRequest.getId();
		UserPicture userPicture = userPictureService.getUserPicture(uid, id);
		if (userPicture == null) {
			throw new BusinessException(StatusCode.FORBIDDEN, "未找到该图片");
		}
		String pid = userPicture.getPid();
		String fileName = userPicture.getFileName();
		ShareMode shareMode = ShareMode.of(picturePreviewRequest.getShareMode());
		Picture picture = pictureService.getPicture(pid);
		File pictureFile;
		switch (shareMode) {
			case PUBLIC -> pictureFile = new File(picture.getWatermark());
			case PUBLIC_COMPRESS -> pictureFile = new File(picture.getThumbnail());
			default -> pictureFile = new File(picture.getOriginal());
		}
		try {
			InputStream is = new FileInputStream(pictureFile);
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_TYPE, Files.probeContentType(pictureFile.toPath()));
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline;filename=" + pictureFile.getName());
			InputStreamResource inputStreamResource = new InputStreamResource(is);
			return new ResponseEntity<>(inputStreamResource, headers, StatusCode.OK);
		} catch (Throwable e) {
			log.error("预览系统异常", e);
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "预览系统异常");
		}
	}
}
