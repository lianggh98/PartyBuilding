package com.partybuilding.foundation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.partybuilding.foundation.common.BizException;
import com.partybuilding.foundation.domain.OrgType;
import com.partybuilding.foundation.domain.PartyMember;
import com.partybuilding.foundation.domain.PartyOrg;
import com.partybuilding.foundation.mapper.PartyMemberMapper;
import com.partybuilding.foundation.mapper.PartyOrgMapper;
import com.partybuilding.foundation.security.AuthUser;
import com.partybuilding.foundation.web.dto.OrgNode;
import com.partybuilding.foundation.web.dto.OrgRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrgService {

    private final PartyOrgMapper partyOrgMapper;
    private final PartyMemberMapper partyMemberMapper;
    private final DataScopeService dataScopeService;

    public OrgService(PartyOrgMapper partyOrgMapper, PartyMemberMapper partyMemberMapper, DataScopeService dataScopeService) {
        this.partyOrgMapper = partyOrgMapper;
        this.partyMemberMapper = partyMemberMapper;
        this.dataScopeService = dataScopeService;
    }

    public List<OrgNode> tree() {
        List<PartyOrg> orgs = dataScopeService.visibleOrgs().stream()
                .sorted(Comparator.comparing(PartyOrg::getSortNo).thenComparing(PartyOrg::getId))
                .toList();
        Map<Long, OrgNode> nodes = new HashMap<>();
        for (PartyOrg org : orgs) {
            nodes.put(org.getId(), OrgNode.from(org));
        }
        List<OrgNode> roots = new ArrayList<>();
        for (PartyOrg org : orgs) {
            OrgNode node = nodes.get(org.getId());
            OrgNode parent = org.getParentId() == null ? null : nodes.get(org.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.children().add(node);
            }
        }
        return roots;
    }

    public List<OrgNode> flat() {
        return dataScopeService.visibleOrgs().stream()
                .sorted(Comparator.comparing(PartyOrg::getSortNo).thenComparing(PartyOrg::getId))
                .map(OrgNode::from)
                .toList();
    }

    @Transactional
    public OrgNode create(OrgRequest request) {
        dataScopeService.requireWrite();
        AuthUser user = dataScopeService.current();
        OrgType type = request.orgType();
        if (type == OrgType.COMMITTEE) {
            if (!user.role().isSystemAdmin()) {
                throw BizException.forbidden("只有系统管理员可以建立党委");
            }
            if (request.parentId() != null) {
                throw BizException.badRequest("党委是根组织");
            }
        } else {
            if (request.parentId() == null) {
                throw BizException.badRequest("党总支和党支部必须选择上级组织");
            }
            dataScopeService.requireOrgVisible(request.parentId());
            PartyOrg parent = requireOrg(request.parentId());
            if (type == OrgType.GENERAL_BRANCH && parent.getOrgType() != OrgType.COMMITTEE) {
                throw BizException.badRequest("党总支的上级必须是党委");
            }
            if (type == OrgType.BRANCH && parent.getOrgType() == OrgType.BRANCH) {
                throw BizException.badRequest("党支部下面不能再挂党组织");
            }
        }
        PartyOrg org = new PartyOrg();
        org.setParentId(request.parentId());
        org.setName(request.name().trim());
        org.setOrgType(type);
        org.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        partyOrgMapper.insert(org);
        return OrgNode.from(org);
    }

    @Transactional
    public OrgNode rename(Long id, String name) {
        dataScopeService.requireWrite();
        dataScopeService.requireOrgVisible(id);
        PartyOrg org = requireOrg(id);
        org.setName(name.trim());
        partyOrgMapper.updateById(org);
        return OrgNode.from(org);
    }

    @Transactional
    public void delete(Long id) {
        dataScopeService.requireWrite();
        dataScopeService.requireOrgVisible(id);
        AuthUser user = dataScopeService.current();
        if (id.equals(user.orgId())) {
            throw BizException.badRequest("不能删除自己所属的党组织");
        }
        PartyOrg org = requireOrg(id);
        if (org.getOrgType() == OrgType.COMMITTEE && !user.role().isSystemAdmin()) {
            throw BizException.forbidden("只有系统管理员可以删除党委");
        }
        Long children = partyOrgMapper.selectCount(new LambdaQueryWrapper<PartyOrg>().eq(PartyOrg::getParentId, id));
        if (children != null && children > 0) {
            throw BizException.badRequest("请先删除下级组织");
        }
        Long members = partyMemberMapper.selectCount(new LambdaQueryWrapper<PartyMember>().eq(PartyMember::getOrgId, id));
        if (members != null && members > 0) {
            throw BizException.badRequest("该组织下还有党员，不能删除");
        }
        partyOrgMapper.deleteById(id);
    }

    public PartyOrg requireOrg(Long id) {
        PartyOrg org = partyOrgMapper.selectById(id);
        if (org == null) {
            throw BizException.notFound("党组织不存在");
        }
        return org;
    }
}
