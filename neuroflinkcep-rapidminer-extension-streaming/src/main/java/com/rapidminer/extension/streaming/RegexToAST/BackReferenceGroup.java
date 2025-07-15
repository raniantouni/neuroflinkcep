package com.rapidminer.extension.streaming.RegexToAST;

public class BackReferenceGroup extends Node {

    private int groupNumber;

    public BackReferenceGroup(int groupNumber) {
        this.groupNumber = groupNumber;
    }

    public int getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(int groupNumber) {
        this.groupNumber = groupNumber;
    }

    @Override
    public String toString() {
        return "\\" + groupNumber;
    }
}
