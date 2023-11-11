package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.mapper.UserPictureMapper;
import cc.vastsea.toyou.model.entity.UserPicture;
import cc.vastsea.toyou.service.UserPictureService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class UserPictureServiceImpl extends ServiceImpl<UserPictureMapper, UserPicture> implements UserPictureService {
	@Resource
	private UserPictureMapper userPictureMapper;

}
