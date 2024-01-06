package cc.vastsea.toyou.service;

import cc.vastsea.toyou.model.dto.verify.RealNameVerifyResponse;
import cc.vastsea.toyou.model.entity.RealName;
import cc.vastsea.toyou.model.entity.verify.RealNameVerifyInfo;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletRequest;

public interface RealNameVerifyService {
    RealNameVerifyResponse initFaceVerify(RealNameVerifyInfo realNameVerifyInfo);
    RealNameVerifyResponse startVerify(String certifyId,String url);
    boolean verifyResultQuery(HttpServletRequest httpServletRequest, String certifyId);
}
