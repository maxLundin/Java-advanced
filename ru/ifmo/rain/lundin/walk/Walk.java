package ru.ifmo.rain.lundin.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class Walk {

    static boolean checkArgs(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Wrong number of arguments!");
            System.err.println("Input type:");
            System.err.println("java Walk <входной файл> <выходной файл>");
            return false;
        }
        return true;
    }

    static void createDirectory(String filePath) {
        String directoryName = (new File(filePath)).getParent();
        if (directoryName != null) {
            File directory = new File(directoryName);
            directory.mkdirs();
        }
    }

    public static void main(String[] args) {
        if (!checkArgs(args)) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(args[0], StandardCharsets.UTF_8))) {
            createDirectory(args[1]);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(args[1], StandardCharsets.UTF_8))) {
                while (reader.ready()) {
                    String fileName = reader.readLine();
                    writer.write(Hash.hashFileByName(fileName));
                    writer.write(" " + fileName);
                    writer.newLine();
                }
            } catch (FileNotFoundException e) {
                System.out.println("No such file: " + args[1]);
            } catch (UnsupportedEncodingException e) {
                System.out.println("Unsupported encoding");
            } catch (IOException e) {
                System.out.println("Reading error");
            }
        } catch (FileNotFoundException e) {
            System.out.println("No such file " + args[0]);
        } catch (UnsupportedEncodingException e) {
            System.out.println("Unsupported encoding");
        } catch (IOException e) {
            System.out.println("Reading error");
        }
    }
}
