package com.partybuilding.foundation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.partybuilding.foundation.common.BizException;
import com.partybuilding.foundation.domain.OrgType;
import com.partybuilding.foundation.domain.PartyMember;
import com.partybuilding.foundation.domain.PartyOrg;
import com.partybuilding.foundation.mapper.PartyMemberMapper;
import com.partybuilding.foundation.web.dto.MemberRequest;
import com.partybuilding.foundation.web.dto.MemberResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MemberService {

    private final PartyMemberMapper partyMemberMapper;
    private final OrgService orgService;
    private final DataScopeService dataScopeService;

    public MemberService(PartyMemberMapper partyMemberMapper, OrgService orgService, DataScopeService dataScopeService) {
        this.partyMemberMapper = partyMemberMapper;
        this.orgService = orgService;
        this.dataScopeService = dataScopeService;
    }

    public List<MemberResponse> list(String name, Long orgId) {
        if (orgId != null) {
            dataScopeService.requireOrgVisible(orgId);
        }
        List<Long> scope = dataScopeService.visibleOrgs().stream().map(PartyOrg::getId).toList();
        if (scope.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<PartyMember> query = new LambdaQueryWrapper<PartyMember>()
                .in(PartyMember::getOrgId, scope)
                .orderByDesc(PartyMember::getId);
        if (orgId != null) {
            query.eq(PartyMember::getOrgId, orgId);
        }
        if (StringUtils.hasText(name)) {
            query.like(PartyMember::getName, name.trim());
        }
        Map<Long, PartyOrg> orgs = dataScopeService.visibleOrgs().stream()
                .collect(Collectors.toMap(PartyOrg::getId, Function.identity()));
        return partyMemberMapper.selectList(query).stream()
                .map(member -> MemberResponse.from(member, orgs.get(member.getOrgId())))
                .toList();
    }

    public MemberResponse get(Long id) {
        PartyMember member = requireVisible(id);
        PartyOrg org = orgService.requireOrg(member.getOrgId());
        return MemberResponse.from(member, org);
    }

    @Transactional
    public MemberResponse create(MemberRequest request) {
        dataScopeService.requireWrite();
        PartyOrg org = requireBranch(request.orgId());
        PartyMember member = new PartyMember();
        apply(member, request, org.getId());
        if (!StringUtils.hasText(member.getStatus())) {
            member.setStatus("ACTIVE");
        }
        partyMemberMapper.insert(member);
        return MemberResponse.from(member, org);
    }

    @Transactional
    public MemberResponse update(Long id, MemberRequest request) {
        dataScopeService.requireWrite();
        PartyMember member = requireVisible(id);
        PartyOrg org = requireBranch(request.orgId());
        apply(member, request, org.getId());
        partyMemberMapper.updateById(member);
        return MemberResponse.from(member, org);
    }

    private PartyMember requireVisible(Long id) {
        PartyMember member = partyMemberMapper.selectById(id);
        if (member == null) {
            throw BizException.notFound("党员不存在");
        }
        dataScopeService.requireOrgVisible(member.getOrgId());
        return member;
    }

    private PartyOrg requireBranch(Long orgId) {
        dataScopeService.requireOrgVisible(orgId);
        PartyOrg org = orgService.requireOrg(orgId);
        if (org.getOrgType() != OrgType.BRANCH) {
            throw BizException.badRequest("党员必须隶属于党支部");
        }
        return org;
    }

    private void apply(PartyMember member, MemberRequest request, Long orgId) {
        member.setName(request.name().trim());
        member.setGender(blankToNull(request.gender()));
        member.setPhone(blankToNull(request.phone()));
        member.setMemberNo(blankToNull(request.memberNo()));
        member.setOrgId(orgId);
        member.setStatus(StringUtils.hasText(request.status()) ? request.status().trim() : "ACTIVE");
        member.setJoinedAt(request.joinedAt());
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
