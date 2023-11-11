package cc.vastsea.toyou.controler;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.model.dto.UploadPictureRequest;
import cc.vastsea.toyou.model.entity.Picture;
import cc.vastsea.toyou.service.PictureService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {
	@Resource
	private PictureService pictureService;

	@PostMapping("")
	public ResponseEntity<String> uploadPicture(UploadPictureRequest uploadPictureRequest, @RequestBody MultipartFile file, HttpServletRequest request) {
		Picture picture;
		try {
			picture = pictureService.uploadPicture(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new ResponseEntity<>(String.valueOf(picture.getSize()), null, StatusCode.CREATED);
	}
}
