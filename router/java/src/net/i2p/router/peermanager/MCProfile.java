package net.i2p.router.peermanager;

import java.util.Deque;
import java.util.ArrayDeque;
import java.util.Arrays;

class MCProfile {

    private static final double DEFAULT_SS = 0.5;
    private static final double DEFAULT_SR = 0;
    private static final double DEFAULT_SF = 0.5;

    private static final double DEFAULT_RS = 0.5;
    private static final double DEFAULT_RR = 0;
    private static final double DEFAULT_RF = 0.5;

    private static final double DEFAULT_FS = 0.5;
    private static final double DEFAULT_FR = 0;
    private static final double DEFAULT_FF = 0.5;

    private final int minHistory, maxHistory;
    private final Deque<PeerAttempt> history;

    private Transition[] outgoingTransitions;

    private Transition[] fromS, fromR, fromF;

    private String toString;

    MCProfile(int minHistory, int maxHistory) {
        this.minHistory = minHistory;
        this.maxHistory = maxHistory;

        this.history = new ArrayDeque(maxHistory);

        fromS = new Transition[3];
        fromS[0] = new Transition(PeerAttempt.SUCCESS, DEFAULT_SS);
        fromS[1] = new Transition(PeerAttempt.REJECT, DEFAULT_SR);
        fromS[2] = new Transition(PeerAttempt.FAILED, DEFAULT_SF);
        Arrays.sort(fromS);
        fromS[1].probability += fromS[0].probability;
        fromS[2].probability += fromS[1].probability;

        fromR = new Transition[3];
        fromR[0] = new Transition(PeerAttempt.SUCCESS, DEFAULT_RS);
        fromR[1] = new Transition(PeerAttempt.REJECT, DEFAULT_RR);
        fromR[2] = new Transition(PeerAttempt.FAILED, DEFAULT_RF);
        Arrays.sort(fromR);
        fromR[1].probability += fromR[0].probability;
        fromR[2].probability += fromR[1].probability;

        fromF = new Transition[3];
        fromF[0] = new Transition(PeerAttempt.SUCCESS, DEFAULT_FS);
        fromF[1] = new Transition(PeerAttempt.REJECT, DEFAULT_FR);
        fromF[2] = new Transition(PeerAttempt.FAILED, DEFAULT_FF);
        Arrays.sort(fromF);
        fromF[1].probability += fromF[0].probability;
        fromF[2].probability += fromF[1].probability;

        this.outgoingTransitions = fromS;
    }


    public synchronized PeerAttempt predict() {
        double random = Math.random();
        if (random < outgoingTransitions[0].probability)
            return outgoingTransitions[0].state;
        if (random < outgoingTransitions[1].probability)
            return outgoingTransitions[1].state;
        return outgoingTransitions[2].state;
    }

    public synchronized void observe(PeerAttempt state) {
        if (history.size() == maxHistory)
            history.removeFirst();
        history.addLast(state);

        if (history.size() < minHistory)
            return;

        // recompute transition prbabilities
        int ss = 0;
        int sr = 0;
        int sf = 0;
        int rs = 0;
        int rr = 0;
        int rf = 0;
        int fs = 0;
        int fr = 0;
        int ff = 0;

        PeerAttempt current = null;
        for (PeerAttempt observed : history) {
            if (current == null) {
                current = observed;
                continue;
            }
            switch(current) {
            case SUCCESS :
                switch(observed) {
                case SUCCESS : ss++; break;
                case REJECT : sr++; break;
                case FAILED : sf++; break;
                }
            break;
            
            case REJECT :
                switch(observed) {
                case SUCCESS : rs++; break;
                case REJECT : rr++; break;
                case FAILED : rf++; break;
                }
            break;
            
            case FAILED :
                switch(observed) {
                case SUCCESS : fs++; break;
                case REJECT : fr++; break;
                case FAILED : ff++; break;
                }
            break;
            }
            current = observed;
        }

        int countS = ss + sr + sf;
        int countR = rs + rr + rf;
        int countF = fs + fs + ff;

        double ssd = DEFAULT_SS;
        double srd = DEFAULT_SR;
        double sfd = DEFAULT_SF;
        if (countS > 0) {
            ssd = ss * 1.0 / countS;
            srd = sr * 1.0 / countS;
            sfd = sf * 1.0 / countS;
        }

        double rsd = DEFAULT_RS;
        double rrd = DEFAULT_RR;
        double rfd = DEFAULT_RF;
        if (countR > 0) {
            rsd = rs * 1.0 / countR;
            rrd = rr * 1.0 / countR;
            rfd = rf * 1.0 / countR;
        }

        double fsd = DEFAULT_FS;
        double frd = DEFAULT_FR;
        double ffd = DEFAULT_FF;
        if (countF > 0) {
            fsd = fs * 1.0 / countF;
            frd = fr * 1.0 / countF;
            ffd = ff * 1.0 / countF;
        }

        toString = String.format("SS:%.2f SR:%.2f SF:%.2f RS:%.2f RR:%.2f RD:%.2f FS:%.2f FR:%.2f FF:%.2f",
            ssd,srd,sfd,rsd,rrd,rfd,fsd,frd,ffd);

        // update transition tables
        fromS[0] = new Transition(PeerAttempt.SUCCESS, ssd);
        fromS[1] = new Transition(PeerAttempt.REJECT, srd);
        fromS[2] = new Transition(PeerAttempt.FAILED, sfd);
        fromR[0] = new Transition(PeerAttempt.SUCCESS, rsd);
        fromR[1] = new Transition(PeerAttempt.REJECT, rrd);
        fromR[2] = new Transition(PeerAttempt.FAILED, rfd);
        fromF[0] = new Transition(PeerAttempt.SUCCESS, fsd);
        fromF[1] = new Transition(PeerAttempt.REJECT, frd);
        fromF[2] = new Transition(PeerAttempt.FAILED, ffd);
        
        Arrays.sort(fromS);
        fromS[1].probability += fromS[0].probability;
        fromS[2].probability += fromS[1].probability;
        
        Arrays.sort(fromS);
        fromR[1].probability += fromR[0].probability;
        fromR[2].probability += fromR[1].probability;

        Arrays.sort(fromS);
        fromR[1].probability += fromR[0].probability;
        fromR[2].probability += fromR[1].probability;

        // select current table
        switch(state) {
        case SUCCESS: outgoingTransitions = fromS; break;
        case REJECT: outgoingTransitions = fromR; break;
        case FAILED: outgoingTransitions = fromF; break;
        }
        
    }

    public String toString() {
        return toString;
    }

    private static class Transition implements Comparable<Transition>{
        private double probability;
        private final PeerAttempt state;
        Transition(PeerAttempt state, double probability) {
            this.state = state;
            this.probability = probability;
        }

        public int compareTo(Transition other) {
            return Double.compare(probability, other.probability);
        }
    }
}
