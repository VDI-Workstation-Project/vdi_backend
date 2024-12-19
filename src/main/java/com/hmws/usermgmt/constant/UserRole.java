package com.hmws.usermgmt.constant;

public enum UserRole {
    INTERN("인턴"),
    JUNIOR("사원"),
    SENIOR("대리"),
    LEAD("과장"),
    MANAGER("팀장"),
    DIRECTOR("이사"),
    VICE_PRESIDENT("부사장"),
    PRESIDENT("사장"),
    CEO("최고경영자 (CEO)"),
    CTO("최고기술책임자 (CTO)");

    private final String role;

    UserRole(String role) {
        this.role = role;
    }

    public String getRole() {
        return role;
    }
}
