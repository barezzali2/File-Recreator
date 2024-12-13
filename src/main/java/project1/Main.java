// Barez Zuber Ali - bz20458@auis.edu.krd
// Bryar Dyar Abubaker - bz20356@auis.edu.krd


package project1;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) throws IOException{

        int[] ports = {1081, 1082, 1083, 1084, 1085};
        Random rand = new Random();
        ClockTracker clock = new ClockTracker();
        Map<Integer, byte[]> receivedBatches = new HashMap<>();

        // References:
        // https://stackoverflow.com/questions/24329007/zeromq-java-performance-on-pub-sub
        // https://javamasterybooks.com/zeromq/2/1/2/
        // https://docs.oracle.com/javase/8/docs/api/java/nio/file/Paths.html

        try (ZContext context = new ZContext()) {
            clock.increment();
            ZMQ.Socket publisher = context.createSocket(SocketType.PUB);
            publisher.bind("tcp://*:1080");

            
            clock.increment();
            ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);
            int[] processesPorts = {2091, 2092, 2093, 2094, 2095};
            for (int processPort : processesPorts) {
                subscriber.connect("tcp://localhost:" + processPort);
            }
            subscriber.subscribe("response".getBytes());

            System.out.println("Publisher is running...");


            System.out.println("Enter the file path to distribute:");
            String filePath = System.console().readLine();



            // Determine file type dynamically
            Path path = Paths.get(filePath);
            String fileType = Files.probeContentType(path);
            String fileExtension = filePath.substring(filePath.lastIndexOf('.')); // Get file extension

            try {
                byte[] fileContent = Files.readAllBytes(Paths.get(filePath));
                int length = fileContent.length;

                // Distribute the batches
                System.out.println("Distributing batches...");
                int batchSize = 10;
                for (int i = 0; i < length; i += batchSize) {
                    int end = Math.min(i + batchSize, length);
                    byte[] batch = Arrays.copyOfRange(fileContent, i, end);
                    int randomPort = ports[rand.nextInt(ports.length)];

                    clock.increment();
                    byte[] message = (randomPort + ":" + clock.getTime() + ":").getBytes();
                    byte[] fullMessage = new byte[message.length + batch.length];
                    System.arraycopy(message, 0, fullMessage, 0, message.length);
                    System.arraycopy(batch, 0, fullMessage, message.length, batch.length);

                    publisher.send(fullMessage);
                    System.out.println("Published batch to port " + randomPort + ": [" + new String(batch, StandardCharsets.UTF_8) + "]"); // logging
                    System.out.println("Lamport Clock: " + clock.getTime());
                }


                // Wait for 15 seconds
                clock.increment();
                System.out.println("Waiting for 15 seconds...");
                Thread.sleep(15000);

                // Collect responses
                System.out.println("Collecting responses...");
                while (true) {
                    clock.increment();
                    byte[] response = subscriber.recv(ZMQ.DONTWAIT);
                    if (response != null) {
                        String header = new String(response, 0, response.length - batchSize, StandardCharsets.UTF_8);
                        String[] parts = header.split(":", 3);
                        int timestamp = Integer.parseInt(parts[1]);
                        byte[] batch = Arrays.copyOfRange(response, header.length(), response.length);

                        clock.update(timestamp);
                        receivedBatches.put(timestamp, batch);
                        System.out.println("Received batch at timestamp " + timestamp + ": [" + new String(batch, StandardCharsets.UTF_8) + "]");
                    } else {
                        break; // Exit the loop if no more responses
                    }
                }

                // Reconstruct the file
                System.out.println("Reconstructing the file...");
                List<Byte> byteList = new ArrayList<>();
                receivedBatches.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        for (byte b : entry.getValue()) {
                            byteList.add(b);
                        }
                    });

                // Convert List<Byte> to byte[]
                byte[] reconstructedFile = new byte[byteList.size()];
                for (int i = 0; i < byteList.size(); i++) {
                    reconstructedFile[i] = byteList.get(i);
                }


                // References:
                // https://www.w3schools.com/java/java_files.asp
                // https://docs.oracle.com/javase/8/docs/api/java/io/FileOutputStream.html
                // https://chatgpt.com/

                // Save the reconstructed file
                String desktopPath = System.getProperty("user.home") + File.separator + "Desktop";
                String fileName = "NewFile" + fileExtension;
                File outputFile = new File(desktopPath, fileName);
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(reconstructedFile);
                    System.out.println("Reconstructed file saved to: " + outputFile.getAbsolutePath());
                }

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        } catch(Exception ex) {
            ex.getStackTrace();
        }
    }
}