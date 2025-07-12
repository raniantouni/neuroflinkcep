package com.rapidminer.extension.streaming.flink.RegexToAST;

public class EventTypeNode extends Node{
    private char value;

    public EventTypeNode(char value) {
        this.value = value;
    }

    public char getValue() {
        return value;
    }

    public void setValue(char value) {
        this.value = value;
    }
}
