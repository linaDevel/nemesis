package ru.linachan.nemesis.gerrit;

public enum ApprovalType {
    CODE_REVIEW("Code-Review"),
    QUALITY_ASSURANCE("Quality-Assurance"),
    VERIFIED("Verified"),
    WORKFLOW("Workflow"),
    SUBMIT("Subm");

    private String approvalType;

    ApprovalType(String approvalType) {
        this.approvalType = approvalType;
    }

    public static ApprovalType getApprovalType(String approvalType) {
        return ApprovalType.valueOf(approvalType.toUpperCase().replace("-", "_"));
    }

    public String getApprovalType() {
        return approvalType;
    }
}