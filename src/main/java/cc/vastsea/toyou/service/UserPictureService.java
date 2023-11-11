package cc.vastsea.toyou.service;

import cc.vastsea.toyou.model.entity.Picture;
import cc.vastsea.toyou.model.entity.UserPicture;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Set;

public interface UserPictureService extends IService<UserPicture> {
    Set<Picture> getUserPictures(long uid);

    long getUserCurrentSize(long uid);
}
