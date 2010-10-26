/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.collector;

import java.io.IOException;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author justin
 */
public class Listener {

    private ExecutorService executor = Executors.newFixedThreadPool(8);
    protected static Map<String, Template> templates = Collections.synchronizedMap(new HashMap<String, Template>());

    public void listen() {
        try {
            launch();
        } catch (SocketException se) {
            se.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void launch() throws SocketException, IOException {
        try {
            DatagramSocket socket = new DatagramSocket(9995);

            for (int i = 0; i < 8; i++) {
                final FlowWorker worker = new FlowWorker(socket);
                executor.execute(worker);
            }
        } catch (BindException be) {
            System.err.println("Address appears to already be in use: " + be.getMessage());
        }
    }
}
