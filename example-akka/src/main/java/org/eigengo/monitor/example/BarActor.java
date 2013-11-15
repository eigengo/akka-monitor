package org.eigengo.monitor.example;

import akka.actor.UntypedActor;

class BarActor extends UntypedActor {
    int shortSleep = 1;

    public void onReceive(Object o) {
        try {
            receiveInt(Integer.valueOf(o.toString()));
        } catch (Exception e) {
            // and ignore it
        }
    }
    void receiveInt(int i) throws InterruptedException {
        if (i > 0) {
            Thread.sleep(shortSleep);
            self().tell((i - 1),self());
        }
        if (i == 0) {
            System.out.println("Bar done.");
        }
    }
}
