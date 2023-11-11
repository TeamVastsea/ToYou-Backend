package cc.vastsea.toyou.service;

import cc.vastsea.toyou.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.concurrent.Future;

public interface PictureService extends IService<Picture> {
	@Async("asyncTaskExecutor")
	Future<Picture> uploadPicture(MultipartFile file) throws IOException;
}
