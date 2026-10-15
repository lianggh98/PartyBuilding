package com.partybuilding.foundation.web.dto;

import com.partybuilding.foundation.domain.PartyOrg;
import com.partybuilding.foundation.domain.RoleCode;
import com.partybuilding.foundation.domain.SysUser;

public record UserProfile(
        Long id,
        String username,
        String displayName,
        RoleCode role,
        Long orgId,
        String orgName,
        boolean canWrite
) {
    public static UserProfile from(SysUser user, PartyOrg org) {
        return new UserProfile(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRoleCode(),
                user.getOrgId(),
                org == null ? null : org.getName(),
                user.getRoleCode().canWrite());
    }
}
