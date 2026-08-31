package org.example.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.AuthorizationService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.authorization.Authorization;
import org.camunda.bpm.engine.authorization.Permission;
import org.camunda.bpm.engine.authorization.Permissions;
import org.camunda.bpm.engine.authorization.Resource;
import org.camunda.bpm.engine.authorization.Resources;
import org.camunda.bpm.engine.identity.Group;
import org.camunda.bpm.engine.identity.User;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class CamundaIdentityConfig {

    public static final String GROUP_USER = "user";
    public static final String GROUP_MODERATOR = "moderator";

    private static final List<String> USER_PROCESSES = List.of(
            "video-upload-process",
            "video-publish-process",
            "video-list-process",
            "video-drafts-process",
            "video-get-process",
            "video-published-list-process",
            "monetization-request-process",
            "monetization-method-process",
            "monetization-list-process",
            "monetization-get-process"
    );

    private static final List<String> MODERATOR_PROCESSES = List.of(
            "auth-register-process",
            "auth-login-process",
            "moderation-pending-videos-process",
            "moderation-pending-methods-process",
            "user-list-process",
            "user-get-process",
            "video-published-list-process"
    );

    private static final List<String> MODERATOR_REVIEW_PROCESSES = List.of(
            "video-publish-process",
            "monetization-method-process"
    );

    /**
     * Процессы, задачи которых раздаются через camunda:candidateGroups="moderator".
     * Это общая очередь модерации без конкретного исполнителя, поэтому права на задачи
     * выдаются группе явно.
     */
    private static final List<String> MODERATOR_QUEUE_PROCESSES = List.of(
            "video-publish-process",
            "monetization-method-process",
            "moderation-pending-videos-process",
            "moderation-pending-methods-process"
    );

    /**
     * Права на PROCESS_DEFINITION действуют на все экземпляры процесса, поэтому
     * READ_TASK/UPDATE_TASK/TASK_WORK сюда не входят: иначе каждый участник группы видел бы
     * в Tasklist чужие задачи. Свои задачи видны по авторизации, которую движок создаёт
     * на assignee при назначении.
     */
    private static final Permission[] OWNER_PERMISSIONS = {
            Permissions.READ,
            Permissions.CREATE_INSTANCE,
            Permissions.READ_INSTANCE,
            Permissions.UPDATE_INSTANCE,
            Permissions.DELETE_INSTANCE,
            Permissions.READ_HISTORY
    };

    private static final Permission[] REVIEWER_PERMISSIONS = {
            Permissions.READ,
            Permissions.READ_INSTANCE,
            Permissions.UPDATE_INSTANCE,
            Permissions.READ_HISTORY
    };

    private static final Permission[] QUEUE_TASK_PERMISSIONS = {
            Permissions.READ_TASK,
            Permissions.UPDATE_TASK,
            Permissions.TASK_WORK
    };

    private final IdentityService identityService;
    private final AuthorizationService authorizationService;

    @EventListener(ApplicationReadyEvent.class)
    public void initCamundaIdentity() {
        createGroupIfAbsent(GROUP_MODERATOR, "Moderator");
        createGroupIfAbsent(GROUP_USER, "User");
        createAdminUserIfAbsent();
        initAuthorizations();
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
        if (identityService.createUserQuery().userId(GROUP_MODERATOR).count() == 0) {
            User moderatorUser = identityService.newUser(GROUP_MODERATOR);
            moderatorUser.setFirstName("Moderator");
            moderatorUser.setLastName("User");
            moderatorUser.setEmail("moderator@example.com");
            moderatorUser.setPassword("moderator");
            identityService.saveUser(moderatorUser);
            identityService.createMembership(GROUP_MODERATOR, GROUP_MODERATOR);
            log.info("Camunda user 'moderator' created and added to group 'moderator'");
        }

        if (identityService.createUserQuery().userId("admin").count() == 0) {
            User adminUser = identityService.newUser("admin");
            adminUser.setFirstName("Admin");
            adminUser.setLastName("User");
            adminUser.setEmail("admin@example.com");
            adminUser.setPassword("admin");
            identityService.saveUser(adminUser);
            identityService.createMembership("admin", GROUP_MODERATOR);
            log.info("Camunda user 'admin' created and added to group 'moderator'");
        }
    }

    private void initAuthorizations() {
        for (String groupId : List.of(GROUP_USER, GROUP_MODERATOR)) {
            grant(groupId, Resources.APPLICATION, "tasklist", Permissions.ACCESS);
            grant(groupId, Resources.FILTER, "*", Permissions.READ);
            grant(groupId, Resources.PROCESS_INSTANCE, "*", Permissions.CREATE);
        }

        Map<String, Set<Permission>> userGrants = new LinkedHashMap<>();
        collect(userGrants, USER_PROCESSES, OWNER_PERMISSIONS);

        Map<String, Set<Permission>> moderatorGrants = new LinkedHashMap<>();
        collect(moderatorGrants, MODERATOR_PROCESSES, OWNER_PERMISSIONS);
        collect(moderatorGrants, MODERATOR_REVIEW_PROCESSES, REVIEWER_PERMISSIONS);
        collect(moderatorGrants, MODERATOR_QUEUE_PROCESSES, QUEUE_TASK_PERMISSIONS);

        userGrants.forEach((processKey, permissions) ->
                grant(GROUP_USER, Resources.PROCESS_DEFINITION, processKey, permissions));
        moderatorGrants.forEach((processKey, permissions) ->
                grant(GROUP_MODERATOR, Resources.PROCESS_DEFINITION, processKey, permissions));
    }

    private void collect(Map<String, Set<Permission>> target, List<String> processKeys, Permission[] permissions) {
        for (String processKey : processKeys) {
            target.computeIfAbsent(processKey, key -> new LinkedHashSet<>()).addAll(List.of(permissions));
        }
    }

    private void grant(String groupId, Resource resource, String resourceId, Permission... permissions) {
        grant(groupId, resource, resourceId, List.of(permissions));
    }

    /**
     * Приводит грант к ровно заданному набору прав: существующая авторизация
     * перезаписывается, иначе снятые в коде права остались бы в БД навсегда.
     */
    private void grant(String groupId, Resource resource, String resourceId, Collection<Permission> permissions) {
        Permission[] granted = permissions.toArray(new Permission[0]);

        List<Authorization> existing = authorizationService.createAuthorizationQuery()
                .authorizationType(Authorization.AUTH_TYPE_GRANT)
                .groupIdIn(groupId)
                .resourceType(resource)
                .resourceId(resourceId)
                .list();

        if (existing.isEmpty()) {
            Authorization authorization = authorizationService.createNewAuthorization(Authorization.AUTH_TYPE_GRANT);
            authorization.setGroupId(groupId);
            authorization.setResource(resource);
            authorization.setResourceId(resourceId);
            authorization.setPermissions(granted);
            authorizationService.saveAuthorization(authorization);
            log.info("Authorization granted: group='{}', resource='{}', resourceId='{}'",
                    groupId, resource.resourceName(), resourceId);
            return;
        }

        for (Authorization authorization : existing) {
            authorization.setPermissions(granted);
            authorizationService.saveAuthorization(authorization);
        }
    }
}
