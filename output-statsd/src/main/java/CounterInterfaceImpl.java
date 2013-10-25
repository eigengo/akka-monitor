package com.eigengo.monitor.output.statsd;

import com.eigengo.monitor.output.api.CounterInterface;
import com.timgroup.statsd.NonBlockingStatsDClient;
import com.timgroup.statsd.StatsDClient;

public class CounterInterfaceImpl implements CounterInterface {
    private static final StatsDClient statsd = new NonBlockingStatsDClient("", "localhost", 8125, new String[]{"tag:value"});

    static void incrementCounter(String s, int i) {
        for (int j = 0; j < i; j++) {incrementCounter(s);}
    }

    static void incrementCounter(String s) {
        statsd.incrementCounter(s);
    }

    static void decrementCounter(String s, int i) {
        for (int j = 0; j < i; j++) {decrementCounter(s);}
    }

    static void decrementCounter(String s) {
        statsd.decrementCounter(s);
    }



}
