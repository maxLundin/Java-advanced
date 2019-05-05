package ru.ifmo.rain.lundin.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.PortUnreachableException;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HelloUDPServer implements HelloServer, AutoCloseable {
    private ExecutorService receiver;
    private ExecutorService workers;
    private DatagramSocket socket;

    /**
     * Starts a new Hello server.
     *
     * @param port    server port.
     * @param threads number of working threads.
     */
    @Override
    public void start(int port, int threads) {
        receiver = Executors.newSingleThreadExecutor();
        workers = Executors.newFixedThreadPool(threads);
        int bufSize;
        try {
            socket = new DatagramSocket(port);
            bufSize = socket.getReceiveBufferSize();
        } catch (SocketException e) {
            System.err.println("Socket exception: cant create socket");
            e.printStackTrace();
            return;
        }
        receiver.submit(() -> {
            if (!socket.isClosed() && !Thread.currentThread().isInterrupted()) {
                do {
                    final DatagramPacket packet = HelloHelper.getDatagramPacket(bufSize);
                    try {
                        socket.receive(packet);
                        workers.submit(() -> runnableRun(packet));
                    } catch (PortUnreachableException e) {
                        System.err.println("Port unreachable on socket: " + socket.toString() + " with port " + port);
                    } catch (SocketException e) {
                        if (!socket.isClosed()) {
                            System.err.println("Socket Exception on socket: " + socket.toString() + " with port " + port);
                        }
                    } catch (IOException e) {
                        System.err.println("IOException on socket: " + socket.toString() + " with port " + port);
                    }
                } while (!socket.isClosed() && !Thread.currentThread().isInterrupted());
            }
        });

    }

    /**
     * Stops server and deallocates all resources.
     */
    @Override
    public void close() {
        receiver.shutdownNow();
        workers.shutdownNow();

        socket.close();
    }

    private void runnableRun(final DatagramPacket dp) {
        final String receivedMsg = HelloHelper.getResult(dp);
        final String sendMsg = "Hello, " + receivedMsg;
        final DatagramPacket sendDatagramPacket = HelloHelper.getDatagramPacket(sendMsg.getBytes(StandardCharsets.UTF_8), dp.getSocketAddress());
        try {
            socket.send(sendDatagramPacket);
        } catch (IOException e) {
            System.err.println("Error sending package to:" + dp.getSocketAddress());
        }
    }


    public static void main(String[] args) {
        Argument arg;
        try {
            arg = HelloHelper.checkArguments(args, "server");
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }

        try (HelloServer server = new HelloUDPServer()) {
            server.start(arg.portNumber, arg.numberOfThreads);
        }
    }
}
