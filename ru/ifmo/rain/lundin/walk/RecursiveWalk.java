package ru.ifmo.rain.lundin.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;


public class RecursiveWalk {

    private static void listAll(String path, BufferedWriter writer) throws IOException {
        File dir = new File(path);
        if (!dir.isDirectory()) {
            writer.write(Hash.hashFileByName(dir.getPath()));
            writer.write(" " + dir.getPath());
            writer.newLine();
            return;
        }
        DirectoryStream<Path> list = Files.newDirectoryStream(dir.toPath());
        for (Path f : list) {
            if (Files.isRegularFile(f)) {
                writer.write(Hash.hashFileByName(f.toFile().getPath()));
                writer.write(" " + f.toFile().getPath());
                writer.newLine();
            } else {
                listAll(f.toFile().getPath(), writer);
            }
        }
    }

    public static void main(String[] args) {
        if (!Walk.checkArgs(args)) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(args[0], StandardCharsets.UTF_8))) {
            Walk.createDirectory(args[1]);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(args[1], StandardCharsets.UTF_8))) {
                while (reader.ready()) {
                    String fileName = reader.readLine();
                    listAll(fileName, writer);
                }
            } catch (FileNotFoundException e) {
                System.out.println("No such file: " + args[1]);
            } catch (UnsupportedEncodingException e) {
                System.out.println("Unsupported encoding in file: " + args[1]);
            } catch (IOException e) {
                System.out.println("Writing error to file: " + args[1]);
            }
        } catch (FileNotFoundException e) {
            System.out.println("No such file " + args[0]);
        } catch (UnsupportedEncodingException e) {
            System.out.println("Unsupported encoding in file: " + args[0]);
        } catch (IOException e) {
            System.out.println("Error reading from file: " + args[0]);
        }
    }

}
