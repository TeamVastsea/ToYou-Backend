package cc.vastsea.toyou.service;

import cc.vastsea.toyou.model.dto.SharePictureRequest;
import cc.vastsea.toyou.model.entity.Share;
import cc.vastsea.toyou.model.entity.UserPicture;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ShareService extends IService<Share> {
	Share addShare(UserPicture userPicture, SharePictureRequest sharePictureRequest);

	void setDownloads(String uuid, long downloads);
}
