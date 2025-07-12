package com.rapidminer.extension.streaming.flink.RegexToAST;

public class RangeNode extends Node {
    char start;
    char end;

    public RangeNode(char start, char end) {
        this.start = start;
        this.end = end;
    }

    public char getStart() {
        return start;
    }

    public void setStart(char start) {
        this.start = start;
    }

    public char getEnd() {
        return end;
    }

    public void setEnd(char end) {
        this.end = end;
    }
}
