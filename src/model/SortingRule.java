package model;

public class SortingRule {
    private String keyword;
    private MailFolder targetFolder;

    public SortingRule(String keyword, MailFolder targetFolder) {
        this.keyword = keyword;
        this.targetFolder = targetFolder;
    }

    public boolean matches(Email email) {
        return email.getContent().toLowerCase().contains(keyword.toLowerCase());
    }

    public MailFolder getTargetFolder() {
        return targetFolder;
    }

    public String getKeyword() {
        return keyword;
    }
}
