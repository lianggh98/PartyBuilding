package com.partybuilding.foundation.service;

import com.partybuilding.foundation.common.BizException;
import com.partybuilding.foundation.domain.PartyOrg;
import com.partybuilding.foundation.mapper.PartyOrgMapper;
import com.partybuilding.foundation.security.AuthUser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DataScopeService {

    private final PartyOrgMapper partyOrgMapper;
    private final CurrentUserService currentUserService;

    public DataScopeService(PartyOrgMapper partyOrgMapper, CurrentUserService currentUserService) {
        this.partyOrgMapper = partyOrgMapper;
        this.currentUserService = currentUserService;
    }

    public AuthUser current() {
        return currentUserService.require();
    }

    public void requireWrite() {
        if (!current().role().canWrite()) {
            throw BizException.forbidden("当前角色只有查看权限");
        }
    }

    public List<PartyOrg> visibleOrgs() {
        List<PartyOrg> all = partyOrgMapper.selectList(null);
        AuthUser user = current();
        if (user.role().isSystemAdmin()) {
            return all;
        }
        if (user.orgId() == null) {
            throw BizException.forbidden("账号未绑定党组织");
        }
        Map<Long, List<PartyOrg>> children = new HashMap<>();
        for (PartyOrg org : all) {
            children.computeIfAbsent(org.getParentId(), key -> new ArrayList<>()).add(org);
        }
        Set<Long> visible = new HashSet<>();
        collect(user.orgId(), children, visible);
        return all.stream().filter(org -> visible.contains(org.getId())).toList();
    }

    public void requireOrgVisible(Long orgId) {
        boolean visible = visibleOrgs().stream().anyMatch(org -> org.getId().equals(orgId));
        if (!visible) {
            throw BizException.forbidden("不能访问该党组织的数据");
        }
    }

    private void collect(Long orgId, Map<Long, List<PartyOrg>> children, Set<Long> visible) {
        if (orgId == null || !visible.add(orgId)) {
            return;
        }
        for (PartyOrg child : children.getOrDefault(orgId, List.of())) {
            collect(child.getId(), children, visible);
        }
    }
}
