package com.partybuilding.foundation.web.dto;

import com.partybuilding.foundation.domain.PartyMember;
import com.partybuilding.foundation.domain.PartyOrg;

import java.time.LocalDate;

public record MemberResponse(
        Long id,
        String name,
        String gender,
        String phone,
        String memberNo,
        Long orgId,
        String orgName,
        String status,
        LocalDate joinedAt
) {
    public static MemberResponse from(PartyMember member, PartyOrg org) {
        return new MemberResponse(
                member.getId(),
                member.getName(),
                member.getGender(),
                member.getPhone(),
                member.getMemberNo(),
                member.getOrgId(),
                org == null ? null : org.getName(),
                member.getStatus(),
                member.getJoinedAt());
    }
}
