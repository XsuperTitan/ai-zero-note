package com.aizeronote.service.impl;

import com.aizeronote.config.RegistrationInviteProperties;
import com.aizeronote.constant.UserConstant;
import com.aizeronote.exception.BusinessException;
import com.aizeronote.exception.ErrorCode;
import com.aizeronote.model.dto.user.UserLoginRequest;
import com.aizeronote.model.dto.user.UserRegisterRequest;
import com.aizeronote.model.entity.User;
import com.aizeronote.model.enums.UserRoleEnum;
import com.aizeronote.model.vo.LoginUserVO;
import com.aizeronote.repository.UserRepository;
import com.aizeronote.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationInviteProperties registrationInviteProperties;

    public UserServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            RegistrationInviteProperties registrationInviteProperties
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.registrationInviteProperties = registrationInviteProperties;
    }

    @Override
    @Transactional
    public long register(UserRegisterRequest request) {
        String userAccount = request.userAccount().trim();
        String userPassword = request.userPassword();
        String checkPassword = request.checkPassword();
        if (!StringUtils.hasText(userAccount) || !StringUtils.hasText(userPassword) || !StringUtils.hasText(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        String inviteCode = request.inviteCode().trim();
        if (!StringUtils.hasText(inviteCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邀请码不能为空");
        }
        String expected = registrationInviteProperties.getInviteCode();
        if (!inviteCode.equals(expected != null ? expected.trim() : "")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邀请码无效");
        }
        if (userRepository.existsByUserAccount(userAccount)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(passwordEncoder.encode(userPassword));
        user.setUserName("User");
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setUserStatus("active");
        userRepository.save(user);
        return user.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public LoginUserVO login(UserLoginRequest request, HttpServletRequest httpRequest) {
        String userAccount = request.userAccount().trim();
        String userPassword = request.userPassword();
        if (!StringUtils.hasText(userAccount) || !StringUtils.hasText(userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        User user = userRepository.findByUserAccount(userAccount)
                .orElseThrow(() -> new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误"));
        if (!"active".equalsIgnoreCase(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号已停用");
        }
        if (!passwordEncoder.matches(userPassword, user.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        httpRequest.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return getLoginUserVO(user);
    }

    @Override
    public boolean logout(HttpServletRequest httpRequest) {
        Object userObj = httpRequest.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        httpRequest.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    @Override
    @Transactional(readOnly = true)
    public User getLoginUser(HttpServletRequest httpRequest) {
        Object userObj = httpRequest.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (!(userObj instanceof User sessionUser) || sessionUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        Long userId = sessionUser.getId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_LOGIN_ERROR));
        if (!"active".equalsIgnoreCase(user.getUserStatus())) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return user;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO vo = new LoginUserVO();
        vo.setId(user.getId());
        vo.setUserAccount(user.getUserAccount());
        vo.setUserName(user.getUserName());
        vo.setUserRole(user.getUserRole());
        vo.setPermissions(user.getPermissions());
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLoggedIn(HttpServletRequest httpRequest) {
        Object userObj = httpRequest.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (!(userObj instanceof User sessionUser) || sessionUser.getId() == null) {
            return false;
        }
        return userRepository.findById(sessionUser.getId())
                .filter(u -> "active".equalsIgnoreCase(u.getUserStatus()))
                .isPresent();
    }
}
