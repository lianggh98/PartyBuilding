package com.partybuilding.foundation.service;

import com.partybuilding.foundation.domain.OrgType;
import com.partybuilding.foundation.domain.PartyOrg;
import com.partybuilding.foundation.domain.RoleCode;
import com.partybuilding.foundation.domain.SysUser;

public final class RoleBinding {

    private RoleBinding() {
    }

    public static boolean matches(SysUser user, PartyOrg org) {
        RoleCode role = user.getRoleCode();
        if (role == null) {
            return false;
        }
        return switch (role) {
            case SYSTEM_ADMIN -> user.getOrgId() == null;
            case COMMITTEE_ADMIN -> org != null && org.getOrgType() == OrgType.COMMITTEE && org.getId().equals(user.getOrgId());
            case GENERAL_ADMIN -> org != null && org.getOrgType() == OrgType.GENERAL_BRANCH && org.getId().equals(user.getOrgId());
            case BRANCH_ADMIN, BRANCH_VIEWER -> org != null && org.getOrgType() == OrgType.BRANCH && org.getId().equals(user.getOrgId());
        };
    }
}
