package cc.vastsea.toyou.controller;

import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.model.dto.verify.RealNameVerifyResponse;
import cc.vastsea.toyou.model.entity.verify.RealNameVerifyInfo;
import cc.vastsea.toyou.service.RealNameVerifyService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/verify")
@Slf4j
public class RealNameVerifyController {

    @Resource
    private RealNameVerifyService realNameVerifyService;

    @PostMapping("/init")
    public ResponseEntity<RealNameVerifyResponse> init(RealNameVerifyInfo realNameVerifyInfo,
                                                       HttpServletRequest request) {
        RealNameVerifyResponse realNameVerifyResponse = realNameVerifyService.initFaceVerify(realNameVerifyInfo);
        return new ResponseEntity<>(realNameVerifyResponse, null, StatusCode.OK);
    }

    @PostMapping("/start")
    public ResponseEntity<RealNameVerifyResponse> start(String certifyId,String url,
                                                        HttpServletRequest request){
        return new ResponseEntity<>(realNameVerifyService.startVerify(certifyId,url),null,StatusCode.OK);
    }

    @PostMapping("/query")
    public ResponseEntity<Boolean> query(String certifyId,
                                         HttpServletRequest request){
        return new ResponseEntity<>(realNameVerifyService.verifyResultQuery(request,certifyId),null,StatusCode.OK);
    }
}
