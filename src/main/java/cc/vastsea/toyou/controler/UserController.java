package cc.vastsea.toyou.controler;

import cc.vastsea.toyou.annotation.AuthCheck;
import cc.vastsea.toyou.common.StatusCode;
import cc.vastsea.toyou.model.dto.EmailCodeGetResponse;
import cc.vastsea.toyou.model.dto.UserCreateRequest;
import cc.vastsea.toyou.model.dto.UserLoginRequest;
import cc.vastsea.toyou.model.dto.UserLoginResponse;
import cc.vastsea.toyou.model.entity.User;
import cc.vastsea.toyou.model.vo.UserVO;
import cc.vastsea.toyou.service.MailService;
import cc.vastsea.toyou.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private MailService mailService;

    @GetMapping("/email/{email}")
    public ResponseEntity<String> getEmailCode(@PathVariable("email") String email, HttpServletRequest request) {
        EmailCodeGetResponse ecr = userService.getEmailCode(email, request);
        if (ecr.isExist()) {
            return new ResponseEntity<>("exists", null, StatusCode.OK);
        }
        mailService.verifyEmail(email, ecr.getCode());
        return new ResponseEntity<>("created", null, StatusCode.CREATED);
    }

    @GetMapping("/{uid}")
    @AuthCheck(any = "user.get")
    public ResponseEntity<UserVO> getUser(@PathVariable("uid") Long uid, HttpServletRequest request) {
        User user = userService.getUserByUid(uid, request);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return new ResponseEntity<>(userVO, null, StatusCode.OK);
    }

    @PostMapping("")
    public ResponseEntity<String> createUser(UserCreateRequest userCreateRequest, HttpServletRequest request) {
        userService.createUser(userCreateRequest, request);
        return new ResponseEntity<>(null, null, StatusCode.CREATED);
    }

    @GetMapping("")
    public ResponseEntity<UserVO> userLogin(String username, String password, HttpServletRequest request) {
        UserLoginRequest userLoginRequest = new UserLoginRequest();
        userLoginRequest.setAccount(username);
        userLoginRequest.setPassword(password);
        UserVO userVO = userService.userLogin(userLoginRequest, request);

        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("token", userService.getToken(userVO.getUid()));

        return new ResponseEntity<>(userVO, headers, StatusCode.OK);
    }
}
