package cc.vastsea.toyou.service;

import cc.vastsea.toyou.model.entity.Picture;

import java.io.IOException;

public interface PictureService {
	public Picture addPicture(String data) throws IOException;
}
