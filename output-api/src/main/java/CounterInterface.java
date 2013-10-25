package com.eigengo.monitor.output.api;

public interface CounterInterface {
    // these should all be static, but can't do that in an interface until java 8. *sadface*
    void incrementCounter(String s, int i);

    void incrementCounter(String s);

    void decrementCounter(String s, int i);

    void decrementCounter(String s);

}