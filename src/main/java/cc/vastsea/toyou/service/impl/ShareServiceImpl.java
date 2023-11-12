package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.mapper.ShareMapper;
import cc.vastsea.toyou.model.dto.SharePictureRequest;
import cc.vastsea.toyou.model.entity.Share;
import cc.vastsea.toyou.model.entity.UserPicture;
import cc.vastsea.toyou.model.enums.ShareMode;
import cc.vastsea.toyou.service.ShareService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class ShareServiceImpl extends ServiceImpl<ShareMapper, Share> implements ShareService {
	@Resource
	private ShareMapper shareMapper;


	@Override
	public Share addShare(UserPicture userPicture, SharePictureRequest sharePictureRequest) {
		ShareMode shareMode = ShareMode.of(sharePictureRequest.getShareMode());
		String password = sharePictureRequest.getPassword();
		Share share = new Share();
		UUID uuid = UUID.randomUUID();
		share.setSid(uuid.toString());
		share.setId(userPicture.getId());
		share.setShareMode(shareMode.getCode());
		if (!StringUtils.isAnyBlank(password)) {
			share.setPassword(password);
		}
		// 计算36小时后的时间戳
		long expireTime = System.currentTimeMillis() + 36 * 60 * 60 * 1000;
		share.setExpiry(expireTime);
		boolean saveResult = this.save(share);
		if (!saveResult) {
			log.error("分享图片失败，share:{}", share);
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "添加失败，数据库错误");
		}
		return share;
	}

	@Override
	public void setDownloads(String uuid, long downloads) {
		Share share = shareMapper.selectById(uuid);
		if (share == null) {
			throw new BusinessException(StatusCode.NOT_FOUND, "分享不存在");
		}
		share.setDownloads(downloads);
		boolean updateResult = this.updateById(share);
		if (!updateResult) {
			log.error("更新分享下载次数失败，share:{}", share);
			throw new BusinessException(StatusCode.INTERNAL_SERVER_ERROR, "更新失败，数据库错误");
		}
	}
}
