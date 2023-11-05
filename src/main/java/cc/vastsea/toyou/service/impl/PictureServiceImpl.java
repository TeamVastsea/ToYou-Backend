package cc.vastsea.toyou.service.impl;

import cc.vastsea.toyou.common.ErrorCode;
import cc.vastsea.toyou.exception.BusinessException;
import cc.vastsea.toyou.mapper.PictureMapper;
import cc.vastsea.toyou.model.entity.Picture;
import cc.vastsea.toyou.service.PictureService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {
	@Override
	public boolean addPicture(String data) {
		if (data.isEmpty()) {
			throw new BusinessException(ErrorCode.PARAMS_ERROR, "body为空");
		}

		Picture picture = new Picture();

		return true;
	}
}
