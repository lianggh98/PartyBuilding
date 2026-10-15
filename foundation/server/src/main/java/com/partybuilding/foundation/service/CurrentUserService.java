package com.partybuilding.foundation.service;

import com.partybuilding.foundation.common.BizException;
import com.partybuilding.foundation.security.AuthUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    public AuthUser require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser user)) {
            throw BizException.unauthorized("未登录");
        }
        return user;
    }
}
