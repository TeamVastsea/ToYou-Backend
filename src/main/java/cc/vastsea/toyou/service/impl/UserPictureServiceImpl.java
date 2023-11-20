package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.mapper.PictureMapper;
import cc.vastsea.toyou.mapper.UserPictureMapper;
import cc.vastsea.toyou.model.entity.Picture;
import cc.vastsea.toyou.model.entity.UserPicture;
import cc.vastsea.toyou.service.UserPictureService;
import cc.vastsea.toyou.util.CaffeineFactory;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserPictureServiceImpl extends ServiceImpl<UserPictureMapper, UserPicture> implements UserPictureService {
	public static final Cache<Long, Set<UserPicture>> userPictures = CaffeineFactory.newBuilder()
			.expireAfterWrite(1, TimeUnit.DAYS)
			.build();
	@Resource
	private UserPictureMapper userPictureMapper;
	@Resource
	private PictureMapper pictureMapper;

	@Override
	public Set<UserPicture> getUserPictures(long uid) {
		Set<UserPicture> pictures = userPictures.getIfPresent(uid);
		if (pictures == null) {
			UserPicture pictureQuery = new UserPicture();
			QueryWrapper<UserPicture> queryWrapper = new QueryWrapper<>(pictureQuery);
			queryWrapper.eq("uid", uid);
			List<UserPicture> lps = userPictureMapper.selectList(queryWrapper);
			pictures = Set.copyOf(lps);
			userPictures.put(uid, pictures);
		}
		return pictures;
	}

	@Override
	public long getUsedStorage(long uid) {
		Set<UserPicture> pictures = getUserPictures(uid);
		long usedStorage = 0;
		for (UserPicture picture : pictures) {
			Picture pictureQuery = new Picture();
			pictureQuery.setPid(picture.getPid());
			Picture p = pictureMapper.selectOne(new QueryWrapper<>(pictureQuery));
			usedStorage += p.getSize();
		}
		return usedStorage;
	}

	@Override
	public void addUserPicture(long uid, String pid, String fileName) {
		UserPicture userPicture = new UserPicture();
		userPicture.setUid(uid);
		userPicture.setPid(pid);
		userPicture.setFileName(fileName);
		boolean saveResult = this.save(userPicture);
		if (!saveResult) {
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "添加失败，数据库错误");
		}
		userPictures.invalidate(uid);
	}

	@Override
	public boolean isExistSameName(long uid, String fileName) {
		Set<UserPicture> pictures = getUserPictures(uid);
		for (UserPicture picture : pictures) {
			if (picture.getFileName().equalsIgnoreCase(fileName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isPictureBelongToUser(long uid, String pid) {
		Set<UserPicture> pictures = getUserPictures(uid);
		for (UserPicture picture : pictures) {
			if (picture.getPid().equals(pid)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public UserPicture getUserPicture(long uid, Long id) {
		Set<UserPicture> pictures = getUserPictures(uid);
		for (UserPicture picture : pictures) {
			if (picture.getId().equals(id)) {
				return picture;
			}
		}
		return null;
	}

	@Override
	public void setDownloads(long id, long downloads) {
		UserPicture userPicture = userPictureMapper.selectById(id);
		if (userPicture == null) {
			throw new BusinessException(StatusCode.NOT_FOUND, "图片不存在");
		}
		userPicture.setDownloads(downloads);
		boolean updateResult = this.updateById(userPicture);
		if (!updateResult) {
			log.error("更新图片下载次数失败，userPicture:{}", userPicture);
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "更新失败，数据库错误");
		}
	}

	@Override
	public void changePictureName(long id, String name) {
		UserPicture userPicture = userPictureMapper.selectById(id);
		if (userPicture == null) {
			throw new BusinessException(StatusCode.NOT_FOUND, "图片不存在");
		}
		userPicture.setFileName(name);
		boolean result = this.updateById(userPicture);
		if (!result) {
			log.error("更改图片名称失败，userPicture:{}", userPicture);
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "更改失败，数据库错误");
		}
	}

	@Override
	public void invalidate(long uid) {
		userPictures.invalidate(uid);
	}
}