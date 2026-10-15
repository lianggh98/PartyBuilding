package com.partybuilding.foundation.seed;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.partybuilding.foundation.domain.RoleCode;
import com.partybuilding.foundation.domain.SysUser;
import com.partybuilding.foundation.mapper.PartyOrgMapper;
import com.partybuilding.foundation.mapper.SysUserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@Profile("dev")
public class DevUserSeeder implements ApplicationRunner {

    private static final Set<String> WEAK_PASSWORDS = Set.of(
            "admin123", "password", "123456", "12345678", "root", "admin", "partybuilding");

    private final Environment environment;
    private final PasswordEncoder passwordEncoder;
    private final SysUserMapper sysUserMapper;
    private final PartyOrgMapper partyOrgMapper;

    public DevUserSeeder(Environment environment, PasswordEncoder passwordEncoder,
                         SysUserMapper sysUserMapper, PartyOrgMapper partyOrgMapper) {
        this.environment = environment;
        this.passwordEncoder = passwordEncoder;
        this.sysUserMapper = sysUserMapper;
        this.partyOrgMapper = partyOrgMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        String password = environment.getProperty("foundation.dev-admin-password", "");
        if (password.length() < 12 || WEAK_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException("DEV_ADMIN_PASSWORD 未设置、短于 12 位，或属于禁止的弱口令");
        }
        if (partyOrgMapper.selectCount(null) == 0) {
            throw new IllegalStateException("开发组织种子不存在。请使用 dev profile，让 Flyway 加载 classpath:db/dev");
        }
        String hash = passwordEncoder.encode(password);
        for (SeedUser seed : SEED_USERS) {
            SysUser existing = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getUsername, seed.username()));
            if (existing != null) {
                continue;
            }
            SysUser user = new SysUser();
            user.setUsername(seed.username());
            user.setPasswordHash(hash);
            user.setDisplayName(seed.displayName());
            user.setRoleCode(seed.role());
            user.setOrgId(seed.orgId());
            user.setEnabled(1);
            sysUserMapper.insert(user);
        }
    }

    private record SeedUser(String username, String displayName, RoleCode role, Long orgId) {
    }

    private static final List<SeedUser> SEED_USERS = List.of(
            new SeedUser("admin", "系统管理员", RoleCode.SYSTEM_ADMIN, null),
            new SeedUser("committee_admin", "学校党委管理员", RoleCode.COMMITTEE_ADMIN, 1L),
            new SeedUser("general_admin", "第一党总支管理员", RoleCode.GENERAL_ADMIN, 2L),
            new SeedUser("branch_admin", "第一党支部管理员", RoleCode.BRANCH_ADMIN, 4L),
            new SeedUser("branch_viewer", "第一党支部只读", RoleCode.BRANCH_VIEWER, 4L)
    );
}
