package com.rapidminer.extension.streaming.flink.RegexToAST;

public class BackReferenceGroup extends Node{

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
}
