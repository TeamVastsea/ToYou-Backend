package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.mapper.PictureMapper;
import cc.vastsea.toyou.model.entity.Picture;
import cc.vastsea.toyou.service.PictureService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.util.DigestUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;

public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {
	@Resource
	private PictureMapper pictureMapper;

	@Override
	public Picture addPicture(String data) throws IOException {
		if (data.isEmpty()) {
			throw new BusinessException(StatusCode.BAD_REQUEST, "body为空");
		}
		byte[] decodedImg = Base64.getDecoder()
				.decode(data.getBytes(StandardCharsets.UTF_8));
		String md5 = DigestUtils.md5DigestAsHex(decodedImg);

		// 检查目标md5图片是否已经存在数据库中
		QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
		queryWrapper.eq("pid", md5);
		Picture pic = pictureMapper.selectOne(queryWrapper);
		if (pic != null) {
			return pic;
		}
		// 将md5取前4位，存入文件夹，防止文件过多
		String dir = md5.substring(0, 4);
		Path path = Paths.get("pictures", dir, md5);
		Files.createDirectories(path.getParent());
		Files.write(path, decodedImg);
		// 获取文件大小，Long
		long size = Files.size(path);

		Picture picture = new Picture();
		picture.setPid(md5);
		picture.setPath(path.toString());
		picture.setSize(size);

		boolean saveResult = this.save(picture);

		return picture;
	}
}
