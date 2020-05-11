package com.makotomiyamoto.locksmithy.debug;

import java.util.HashMap;
import java.util.Map;

public class Main {

    private static Map<Long, String> getValue() {
        Map<Long, String> map = new HashMap<>();
        long id = 5;
        String s = "hello";
        map.put(id, s);
        return map;
    }

    public static void main(String[] args) {

        Map<Long, String> map = getValue();
        long id = 0;
        String s = "";
        for (Long l : map.keySet()) {
            id = l;
            s = map.get(id);
        }
        System.out.println(id);
        System.out.println(s);

    }

}
