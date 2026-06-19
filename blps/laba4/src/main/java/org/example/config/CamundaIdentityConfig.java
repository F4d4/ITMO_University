package org.example.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CamundaIdentityConfig {

    private final IdentityService identityService;

    @EventListener(ApplicationReadyEvent.class)
    public void initCamundaIdentity() {
        createGroupIfAbsent("moderator", "Moderator");
        createGroupIfAbsent("user", "User");
        createAdminUserIfAbsent();
    }

    private void createGroupIfAbsent(String groupId, String groupName) {
        if (identityService.createGroupQuery().groupId(groupId).count() == 0) {
            Group group = identityService.newGroup(groupId);
            group.setName(groupName);
            group.setType("WORKFLOW");
            identityService.saveGroup(group);
            log.info("Camunda group '{}' created", groupId);
        }
    }

    private void createAdminUserIfAbsent() {
        if (identityService.createUserQuery().userId("moderator").count() == 0) {
            User moderatorUser = identityService.newUser("moderator");
            moderatorUser.setFirstName("Moderator");
            moderatorUser.setLastName("User");
            moderatorUser.setEmail("moderator@example.com");
            moderatorUser.setPassword("moderator");
            identityService.saveUser(moderatorUser);
            identityService.createMembership("moderator", "moderator");
            log.info("Camunda user 'moderator' created and added to group 'moderator'");
        }

        if (identityService.createUserQuery().userId("admin").count() == 0) {
            User adminUser = identityService.newUser("admin");
            adminUser.setFirstName("Admin");
            adminUser.setLastName("User");
            adminUser.setEmail("admin@example.com");
            adminUser.setPassword("admin");
            identityService.saveUser(adminUser);
            identityService.createMembership("admin", "moderator");
            log.info("Camunda user 'admin' created and added to group 'moderator'");
        }
    }
}
