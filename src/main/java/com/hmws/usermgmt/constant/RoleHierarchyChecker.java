package com.hmws.usermgmt.constant;

import com.hmws.global.authentication.utils.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class RoleHierarchyChecker {

    private static final List<UserRole> ROLE_HIERARCHY = Arrays.asList(
            UserRole.INTERN,
            UserRole.JUNIOR,
            UserRole.SENIOR,
            UserRole.LEAD,
            UserRole.MANAGER,
            UserRole.DIRECTOR,
            UserRole.VICE_PRESIDENT,
            UserRole.PRESIDENT,
            UserRole.CEO,
            UserRole.CTO
    );

    public boolean isAboveOrEqual(Authentication authentication, String minimumRole) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UserRole userRole = userDetails.getUserRole();
        UserRole requiredRole = UserRole.valueOf(minimumRole);

        int userRoleIndex = ROLE_HIERARCHY.indexOf(userRole);
        int requiredRoleIndex = ROLE_HIERARCHY.indexOf(requiredRole);

        return userRoleIndex >= requiredRoleIndex;

    }
}
