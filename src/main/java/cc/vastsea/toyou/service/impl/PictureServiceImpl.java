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
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Position;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
	public Picture uploadPicture(MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(StatusCode.FORBIDDEN, "图片为空");
		}
		// 获取图片的长度和宽度
		BufferedImage fi = ImageIO.read(file.getInputStream());
		int height = fi.getHeight();
		int width = fi.getWidth();
		byte[] data = file.getBytes();
		// 计算md5
		String md5 = DigestUtils.md5DigestAsHex(data);

		// 使用thumbnailator对该图片进行增加左下角水印
		ByteArrayOutputStream waterStream = new ByteArrayOutputStream();
		// 获取jar包资源文件的logo.svg将其初始化为BufferedImage
		InputStream logo = this.getClass().getClassLoader().getResourceAsStream("logo.png");
		// 将logo缩小为原来的0.2倍
		int logoWidth = width / 8;
		int logoHeight = height / 8;
		BufferedImage bufferedImage = Thumbnails.of(logo).size(logoWidth, logoHeight).asBufferedImage();
		// 初始化Position，为水印位置，为左下角，向上向右移动0.1图片宽度和高度
		Position position = (enclosingWidth, enclosingHeight, width2, height2, insetLeft, insetRight, insetTop, insetBottom) -> {
			int x = insetLeft + width2 / 20;
			int y = enclosingHeight - height2 - insetBottom - height2 / 10;
			return new Point(x, y);
		};
		Thumbnails.of(file.getInputStream()).watermark(position, bufferedImage, 1f).scale(0.85).toOutputStream(waterStream);
		// 写出outputStream
		byte[] waterData = waterStream.toByteArray();
		// 计算水印图md5
		String waterMd5 = DigestUtils.md5DigestAsHex(waterData);

		// 使用thumbnailator对该图片进行压缩，压缩率为0.5
		ByteArrayOutputStream thumbnailStream = new ByteArrayOutputStream();
		Thumbnails.of(file.getInputStream()).scale(0.7).toOutputStream(thumbnailStream);
		byte[] thumbnailData = thumbnailStream.toByteArray();
		// 计算缩略图md5
		String thumbnailMd5 = DigestUtils.md5DigestAsHex(thumbnailData);

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
		// 将水印图md5取前4位
		String waterDir = waterMd5.substring(0, 4);
		// 生成水印图路径
		Path waterPath = Paths.get("pictures", waterDir, waterMd5 + ext);
		Files.createDirectories(waterPath.getParent());
		Files.write(waterPath, waterData);
		// 将缩略图md5取前4位
		String thumbnailDir = thumbnailMd5.substring(0, 4);
		// 生成缩略图路径
		Path thumbnailPath = Paths.get("pictures", thumbnailDir, thumbnailMd5 + ext);
		Files.createDirectories(thumbnailPath.getParent());
		Files.write(thumbnailPath, thumbnailData);

		Picture picture = new Picture();
		picture.setPid(md5);
		picture.setOriginal(path.toString());
		picture.setThumbnail(thumbnailPath.toString());
		picture.setWatermark(waterPath.toString());
		picture.setSize(size);

		boolean saveResult = this.save(picture);

		return picture;
	}
}
