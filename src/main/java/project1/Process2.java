package project1;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public class Process2 {
    public static void main(String[] args) {
        int port = 1082; 
        ClockTracker clock = new ClockTracker();

        try (ZContext context = new ZContext()) {
            clock.increment();
            ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);
            subscriber.connect("tcp://localhost:1080");
            subscriber.subscribe(String.valueOf(port).getBytes());

            clock.increment();
            ZMQ.Socket publisher = context.createSocket(SocketType.PUB);
            publisher.bind("tcp://*:2092");

            System.out.println("Process 2 is listening on port: " + port);

            while (true) {
                byte[] message = subscriber.recv();
                if (message != null) {
                    String header = new String(message, 0, message.length - 10, StandardCharsets.UTF_8);
                    String[] parts = header.split(":", 3);
                    int timestamp = Integer.parseInt(parts[1]);
                    byte[] batch = Arrays.copyOfRange(message, header.length(), message.length);

                    clock.update(timestamp);
                    // System.out.println("Processing batch: " + Arrays.toString(batch));
                    String batchContent = new String(batch, StandardCharsets.UTF_8);
                    System.out.println("Processing batch: [" + (batchContent.isEmpty() ? "Binary data" : batchContent) + "]");


                    
                    String responseHeader = "response:" + clock.getTime() + ":";
                    byte[] responseHeaderBytes = responseHeader.getBytes(StandardCharsets.UTF_8);
                    byte[] response = new byte[responseHeaderBytes.length + batch.length];
                    System.arraycopy(responseHeaderBytes, 0, response, 0, responseHeaderBytes.length);
                    System.arraycopy(batch, 0, response, responseHeaderBytes.length, batch.length);

                    
                    publisher.send(response);
                    // System.out.println("Sent response: " + Arrays.toString(response));
                    System.out.println("Sent response: [" + (batchContent.isEmpty() ? "Binary data" : batchContent) + "]");

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
