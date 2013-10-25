package com.eigengo.monitor.agent.akka;

public class NullCounterInterface implements CounterInterface {
    public void incrementCounter(String s, int i){}

    public void incrementCounter(String s){}

    public void decrementCounter(String s, int i){}

    public void decrementCounter(String s){}
}
