package cc.vastsea.toyou.service;

import cc.vastsea.toyou.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface PictureService extends IService<Picture> {
	Picture uploadPicture(MultipartFile file) throws IOException;
}
