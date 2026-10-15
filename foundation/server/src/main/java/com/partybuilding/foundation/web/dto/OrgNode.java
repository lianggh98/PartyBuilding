package com.partybuilding.foundation.web.dto;

import com.partybuilding.foundation.domain.OrgType;
import com.partybuilding.foundation.domain.PartyOrg;

import java.util.ArrayList;
import java.util.List;

public record OrgNode(
        Long id,
        Long parentId,
        String name,
        OrgType orgType,
        Integer sortNo,
        List<OrgNode> children
) {
    public static OrgNode from(PartyOrg org) {
        return new OrgNode(org.getId(), org.getParentId(), org.getName(), org.getOrgType(), org.getSortNo(), new ArrayList<>());
    }
}
