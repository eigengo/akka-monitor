package com.eigengo.monitor.output.api;

public interface CounterInterface {

    public void incrementCounter(String s, int i);

    public void incrementCounter(String s);

    public void decrementCounter(String s, int i);

    public void decrementCounter(String s);

}