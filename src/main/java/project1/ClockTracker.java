package project1;

public class ClockTracker {
    private int clock;

    public ClockTracker() {
        this.clock = 0;
    }

    // Increment clock for internal events
    public synchronized void increment() {
        this.clock++;
    }

    // Update clock based on received timestamp
    public synchronized void update(int receivedTimestamp) {
        clock = Math.max(this.clock, receivedTimestamp) + 1;
    }

    // Get the current timestamp
    public synchronized int getTime() {
        return this.clock;
    }
}
