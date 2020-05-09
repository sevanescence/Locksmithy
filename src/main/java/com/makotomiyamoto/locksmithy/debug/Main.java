package com.makotomiyamoto.locksmithy.debug;

import java.awt.image.ImagingOpException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {

        BufferedWriter writer = null;

        try {
            writer = new BufferedWriter(new FileWriter(new File("./Main.java")));
            writer.write("dumbass");
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            assert writer != null;
            writer.close();
        }

    }

}
