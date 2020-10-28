package net.i2p.router.peermanager;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

class PowerEstimator {
    private final int minHistory, maxHistory;
    private final Deque<PeerAttempt> history;

    private double alpha;

    private String toString;

    PowerEstimator(int minHistory, int maxHistory) {
        this.minHistory = minHistory;
        this.maxHistory = maxHistory;
        history = new ArrayDeque<>(maxHistory);
    }

    public synchronized void observe(PeerAttempt status) {
        if (history.size() == maxHistory)
            history.removeFirst();
        history.addLast(status);

        if (history.size() < minHistory)
            return;

        List<Integer> samples = new ArrayList<>();
        PeerAttempt current = null;
        int count = 0;
        for (PeerAttempt next : history) {
            if (current == null) {
                if (next != PeerAttempt.FAILED)
                    continue;
                current = next;
                continue;
            }

            if (next != PeerAttempt.FAILED && next != PeerAttempt.REJECT) {
                count++;
            } else {
                if (count > 1)
                    samples.add(count);
                count = 0;
            }
        }

        if (samples.isEmpty())
            return;

        double sumLog = 0;
        for (int sample : samples) {
            sumLog += Math.log(1.0 * sample);
        }

        alpha = 1 + samples.size()/sumLog;

        toString = String.format("alpha:%f N:%d/%d samples:%s", alpha, samples.size(),history.size(),samples);        
    }

    public synchronized PeerAttempt predict() {
        if (history.size() < minHistory)
            return null;

        int x = 0;
        for (Iterator<PeerAttempt> iter = history.descendingIterator(); iter.hasNext();) {
            PeerAttempt recent = iter.next();
            if (recent == PeerAttempt.FAILED || recent == PeerAttempt.REJECT)
                break;
            x++;
        }
        
        if (x < 2)
            return null;

        double px = (alpha - 1) * Math.pow(x + 1, 0 - alpha);   
        if (Math.random() < px)
            return PeerAttempt.SUCCESS;
        return PeerAttempt.FAILED;
    }

    public synchronized String toString() {
        return toString;
    }
}
