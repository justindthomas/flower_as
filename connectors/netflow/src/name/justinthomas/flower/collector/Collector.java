package name.justinthomas.flower.collector;

/**
 *
 * @author justin
 */
public class Collector {
    private static Listener listener;

    public void init() {
        listener = new Listener();
        listener.listen();
    }
}
