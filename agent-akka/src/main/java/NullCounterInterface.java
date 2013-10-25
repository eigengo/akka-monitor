package com.eigengo.monitor.agent.akka;

import com.eigengo.monitor.output.api.CounterInterface;

public class NullCounterInterface implements CounterInterface {
    public void incrementCounter(String s, int i){}

    public void incrementCounter(String s){}

    public void decrementCounter(String s, int i){}

    public void decrementCounter(String s){}
}
