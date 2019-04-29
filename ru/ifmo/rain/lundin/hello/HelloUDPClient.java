package ru.ifmo.rain.lundin.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {

    /**
     * Runs Hello client.
     *
     * @param host     server host
     * @param port     server port
     * @param prefix   request prefix
     * @param threads  number of request threads
     * @param requests number of requests per thread.
     */
    @Override
    public void run(String host, int port, String prefix, int threads, int requests) {
        InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
        ExecutorService workers = Executors.newFixedThreadPool(threads);

        IntStream.range(0, threads).<Runnable>mapToObj(threadNumber -> () -> {
            try (DatagramSocket datagramSocket = new DatagramSocket()) {
                datagramSocket.setSoTimeout(10);

                for (int requestNumber = 0; requestNumber < requests; requestNumber++) {
                    String request = prefix.concat(threadNumber + "").concat("_").concat(requestNumber + "");
                    while (true) {
                        DatagramPacket sendDatagramPacket = HelloHelper.getDatagramPacket(request.getBytes(), inetSocketAddress);
                        DatagramPacket receiveDatagramPacket = HelloHelper.getDatagramPacket(datagramSocket.getReceiveBufferSize());
                        try {
                            datagramSocket.send(sendDatagramPacket);
                            datagramSocket.receive(receiveDatagramPacket);
                            final String receivedMsg = HelloHelper.getResult(receiveDatagramPacket);
                            if (receivedMsg.endsWith(request)) {
                                System.out.println("Requesting to " + host + ", request : " + request);
                                System.out.println("Response from " + host + ", response : " + receivedMsg);
                                break;
                            }
                        } catch (PortUnreachableException e) {
                            System.err.println("Port unreachable");

                        } catch (SocketTimeoutException e) {
                            System.err.println("Socket timout");

                        } catch (IOException e) {
                            System.err.println("IOException");
                        }
                    }
                }
            } catch (SocketException e) {
                System.err.println("Fail with socket");
            }
        }).forEachOrdered(workers::submit);
        workers.shutdown();
        try {
            workers.awaitTermination(threads * requests, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Argument arg;
        try {
            arg = HelloHelper.checkArguments(args, "client");
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }
        HelloClient client = new HelloUDPClient();
        client.run(arg.nameOrIp, arg.portNumber, arg.prefix, arg.numberOfThreads, arg.numberOfRequests);
    }
}
