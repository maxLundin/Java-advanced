package ru.ifmo.rain.lundin.hello;

import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

class Argument {
    String nameOrIp;
    int portNumber;
    String prefix;
    int numberOfThreads;
    int numberOfRequests;
}

class HelloHelper {

    static Argument checkArguments(String[] args, String type) {
        if (args == null) {
            throw new IllegalArgumentException("Argument array is null");
        }
        for (final String arg : args) {
            if (arg == null) {
                throw new IllegalArgumentException("One of the arguments is null");
            }
        }
        Argument argument = new Argument();
        if (type.equalsIgnoreCase("server")) {
            if (args.length != 2) {
                throw new IllegalArgumentException("Wrong argument count for server");
            }
            try {
                argument.portNumber = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Wrong port format : " + args[0] + "for server.");
            }
            try {
                argument.numberOfThreads = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Wrong thread number format : " + args[1] + "for server.");
            }
        } else if (type.equalsIgnoreCase("client")) {
            if (args.length != 5) {
                throw new IllegalArgumentException("Wrong argument count for server");
            }
            argument.nameOrIp = args[0];
            try {
                argument.portNumber = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Wrong port format : " + args[1] + "for client.");
            }
            argument.prefix = args[2];
            try {
                argument.numberOfThreads = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Wrong thread number format : " + args[3] + "for client.");
            }
            try {
                argument.numberOfRequests = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Wrong request number format : " + args[4] + "for client.");
            }
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
        return argument;
    }

    static String getResult(final DatagramPacket dp) {
        return new String(
                dp.getData(),
                dp.getOffset(),
                dp.getLength(),
                StandardCharsets.UTF_8);
    }

    static DatagramPacket getDatagramPacket(int size) {
        byte[] byteArray = new byte[size];
        return new DatagramPacket(byteArray, size);
    }

    static DatagramPacket getDatagramPacket(final byte[] data, final SocketAddress address) {
        return new DatagramPacket(data, 0, data.length, address);
    }
}
