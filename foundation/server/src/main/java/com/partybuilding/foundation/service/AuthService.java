package com.partybuilding.foundation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.partybuilding.foundation.common.BizException;
import com.partybuilding.foundation.domain.PartyOrg;
import com.partybuilding.foundation.domain.SysUser;
import com.partybuilding.foundation.mapper.PartyOrgMapper;
import com.partybuilding.foundation.mapper.SysUserMapper;
import com.partybuilding.foundation.security.AuthUser;
import com.partybuilding.foundation.security.JwtService;
import com.partybuilding.foundation.web.dto.LoginResponse;
import com.partybuilding.foundation.web.dto.UserProfile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final PartyOrgMapper partyOrgMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserService currentUserService;

    public AuthService(SysUserMapper sysUserMapper, PartyOrgMapper partyOrgMapper, PasswordEncoder passwordEncoder,
                       JwtService jwtService, CurrentUserService currentUserService) {
        this.sysUserMapper = sysUserMapper;
        this.partyOrgMapper = partyOrgMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUserService = currentUserService;
    }

    public LoginResponse login(String username, String password) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null || user.getEnabled() == null || user.getEnabled() != 1
                || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        PartyOrg org = user.getOrgId() == null ? null : partyOrgMapper.selectById(user.getOrgId());
        if (!RoleBinding.matches(user, org)) {
            throw BizException.unauthorized("账号与党组织绑定无效");
        }
        UserProfile profile = UserProfile.from(user, org);
        return new LoginResponse(jwtService.issue(user.getId()), profile);
    }

    public UserProfile me() {
        AuthUser current = currentUserService.require();
        SysUser user = sysUserMapper.selectById(current.id());
        PartyOrg org = current.orgId() == null ? null : partyOrgMapper.selectById(current.orgId());
        return UserProfile.from(user, org);
    }
}
