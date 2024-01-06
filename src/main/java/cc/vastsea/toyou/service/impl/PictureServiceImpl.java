package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.mapper.PictureMapper;
import cc.vastsea.toyou.model.entity.Picture;
import cc.vastsea.toyou.service.PictureService;
import cc.vastsea.toyou.util.CaffeineFactory;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.geometry.Position;
import org.springframework.scheduling.annotation.Async;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {
    public static final Cache<String, Picture> pictureCache = CaffeineFactory.newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .build();

    // 清理进行时用户上传图片存储的缓存
    public static final Cache<String, Picture> waitSavePictureCache = CaffeineFactory.newBuilder()
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .build();

    @Resource
    private PictureMapper pictureMapper;


    @Getter
    private boolean allowSaveToDatabase = true;

    // allowSaveToDatabase 是否允许上传图片写入数据库
    public void setAllowSaveToDatabase(boolean allowSaveToDatabase) {
        this.allowSaveToDatabase = allowSaveToDatabase;
        if (allowSaveToDatabase) {
            log.info("end cleanup...");
        } else {
            log.info("start cleanup work");
        }
    }

    @Override
    public Cache<String, Picture> getWaitSavePictureCache() {
        return waitSavePictureCache;
    }

    @Override
    public Picture getPicture(String pid) {
        Picture picture = pictureCache.getIfPresent(pid);
        if (picture != null) {
            return picture;
        }
        picture = pictureMapper.selectById(pid);
        if (picture == null) {
            throw new BusinessException(StatusCode.NOT_FOUND, "图片不存在");
        }
        pictureCache.put(pid, picture);
        return picture;
    }

    @Async("asyncTaskExecutor")
    @Override
    public Future<Picture> uploadPicture(MultipartFile file) throws IOException {
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
        // 将logo缩小为图片的0.2倍
        // width : height = 4.6 : 1
        double maxHeight = Math.min(((double) width * 0.7) / 4.6, (double) height * 0.7);
        double fitHeight = Math.min(Math.max(((double) width * 0.2) / 4.6, (double) height * 0.2), maxHeight);
        int logoHeight = (int) fitHeight;
        int logoWidth = (int) (fitHeight * 4.6);
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
            return CompletableFuture.completedFuture(pic);
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

        // 进行清理时不写入数据库
        if (!isAllowSaveToDatabase()) {
            pictureCache.put(picture.getPid(), picture);
            waitSavePictureCache.put(picture.getPid(), picture);
        } else {
            boolean saveResult = this.save(picture);
            if (!saveResult) {
                throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "添加失败，数据库错误");
            }
        }

        return CompletableFuture.completedFuture(picture);
    }
}
