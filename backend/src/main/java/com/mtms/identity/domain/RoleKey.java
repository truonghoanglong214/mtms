package com.mtms.identity.domain;

/**
 * P0-ORG-05 / BR-ORG-04
 * Core role keys for MTMS multi-tenant, branch-scoped authorization.
 */
public enum RoleKey {
    SYSTEM_ADMIN,
    BRANCH_OWNER,
    MANAGER,
    FINANCE,
    EMPLOYEE,
    INVESTOR
}
