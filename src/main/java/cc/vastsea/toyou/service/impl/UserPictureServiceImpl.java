package cc.vastsea.toyou.service.impl;

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
	@Resource
	private UserPictureMapper userPictureMapper;
	@Resource
	private PictureMapper pictureMapper;
	public static final Cache<Long, Set<Picture>> userPictures = CaffeineFactory.newBuilder()
			.expireAfterWrite(1, TimeUnit.DAYS)
			.build();

	@Override
	public Set<Picture> getUserPictures(long uid){
		Set<Picture> pictures = userPictures.getIfPresent(uid);
		if (pictures == null) {
			Picture pictureQuery = new Picture();
			QueryWrapper<Picture> queryWrapper = new QueryWrapper<>(pictureQuery);
			queryWrapper.eq("uid", uid);
			List<Picture> lps = pictureMapper.selectList(queryWrapper);
			pictures = Set.copyOf(lps);
			userPictures.put(uid, pictures);
		}
		return pictures;
	}

	@Override
	public long getUserCurrentSize(long uid){
		Set<Picture> pictures = getUserPictures(uid);
		long size = pictures.stream().mapToLong(Picture::getSize).sum();
		return size;
	}



}
