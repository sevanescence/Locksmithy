package com.makotomiyamoto.locksmithy.debug;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {

        String sampleCoords = "60-24--45-hello-world";
        // ignore hyphen in split regex if it is at the beginning of the string or after another hyphen

        //System.out.println(Arrays.toString(sampleCoords.split("-")));
        System.out.println(sampleCoords);
        // -(?=[-]) - matches hyphen when a hyphen is found to the right
        // -(?=[\\w]) - matches hyphen when the next character is a letter
        // (?<![-])- - matches hyphen with no hyphen before it
        Pattern pattern = Pattern.compile("(?<!^)(?<![-])-");
        Matcher matcher = pattern.matcher(sampleCoords);
        System.out.println(matcher.replaceAll("_"));

        System.out.println(Arrays.toString(sampleCoords.split("(?<!^)(?<![-])-", 4)));



    }

}
