package cc.vastsea.toyou.service;

import cc.vastsea.toyou.model.entity.UserPicture;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Set;

public interface UserPictureService extends IService<UserPicture> {
	Set<UserPicture> getUserPictures(long uid);

	long getUsedStorage(long uid);

	void addUserPicture(long uid, String pid, String fileName);
}
