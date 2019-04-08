package ru.ifmo.rain.lundin.walk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

class Hash {
    private static String makeHex(int hash) {
        String hexString = Integer.toHexString(hash);
        String zeroString = "00000000";
        return zeroString.substring(hexString.length()) + hexString;
    }

    static String hashFileByName(String fileName) {
        int FNV_32_PRIME = 0x01000193;
        int hval = 0x811c9dc5;
        byte[] buf = new byte[2048];
        try (FileInputStream reader = new FileInputStream(new File(fileName))) {
            int bytesRead = 0;
            while ((bytesRead = reader.read(buf, 0, 2048)) >= 0) {
                for (int i = 0; i < bytesRead; ++i) {
                    hval *= FNV_32_PRIME;
                    hval ^= (buf[i] & 0xff);
                }
            }
            return makeHex(hval);
        } catch (FileNotFoundException e) {
            return makeHex(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return makeHex(0);
    }
}
