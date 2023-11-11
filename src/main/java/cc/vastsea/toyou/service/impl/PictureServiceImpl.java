package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.mapper.PictureMapper;
import cc.vastsea.toyou.model.entity.Picture;
import cc.vastsea.toyou.service.PictureService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {
	@Resource
	private PictureMapper pictureMapper;

	@Override
	public Picture addPicture(MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(StatusCode.FORBIDDEN, "图片为空");
		}
		byte[] data = file.getBytes();
		String md5 = DigestUtils.md5DigestAsHex(data);

		// 检查目标md5图片是否已经存在数据库中
		QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("pid", md5);
		Picture pic = pictureMapper.selectOne(queryWrapper);
		if (pic != null) {
			return pic;
		}
		// 获取图片类型拓展名
		String ext = Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf("."));
		// 将md5取前4位，存入文件夹，防止文件过多
		String dir = md5.substring(0, 4);
		// 生成文件路径
		Path path = Paths.get("pictures", dir, md5 + ext);
		Files.createDirectories(path.getParent());
		Files.write(path, data);
		// 获取文件大小，Long
		long size = file.getSize();

		Picture picture = new Picture();
		picture.setPid(md5);
		picture.setPath(path.toString());
		picture.setSize(size);

		boolean saveResult = this.save(picture);

		return picture;
	}
}
