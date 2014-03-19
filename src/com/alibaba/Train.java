package com.alibaba;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * User: sweeliao@gmail.com
 * Date: 14-3-19
 * Time: 上午9:08
 */
public class Train {
    public static void main(String[] args) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("E:\\workspace\\alibaba\\t_alibaba_data.csv"));
            String line;
            Map<String, Map<String, Map<Integer, HashSet<Integer>>>> table = new HashMap<String, Map<String, Map<Integer, HashSet<Integer>>>>();
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] stat = line.split(",");
                String [] date = stat[3].split("\\W+");
                String user_id = stat[0],
                       brand_id = stat[1],
                       type = stat[2];

                Integer datehash = Integer.valueOf(date[0]) * 31 + Integer.valueOf(date[1]);

                Map<String, Map<Integer, HashSet<Integer>>> oneline = table.get(user_id);
                if (oneline == null) {
                    oneline = new HashMap<String, Map<Integer, HashSet<Integer>>>();
                    Map<Integer, HashSet<Integer>> temp = new HashMap<Integer, HashSet<Integer>>();
                    HashSet<Integer> tempSet = new HashSet<Integer>();
                    tempSet.add(Integer.valueOf(type));
                    temp.put(datehash, tempSet);
                    oneline.put(brand_id, temp);
                }
                else {
                    Map<Integer, HashSet<Integer>> nextline = oneline.get(brand_id);
                    if (nextline == null) {
                        nextline = new HashMap<Integer, HashSet<Integer>>();
                        HashSet<Integer> tempSet = new HashSet<Integer>();
                        tempSet.add(Integer.valueOf(type));
                        nextline.put(datehash, tempSet);
                        oneline.put(brand_id, nextline);
                    }
                    else {
                        HashSet<Integer> thirdline = nextline.get(datehash);
                        if (thirdline == null) {
                            HashSet<Integer> tempSet = new HashSet<Integer>();
                            tempSet.add(Integer.valueOf(type));
                            nextline.put(datehash, tempSet);
                        }
                        else {
                            thirdline.add(Integer.valueOf(type));
                            nextline.put(datehash, thirdline);
                        }
                        oneline.put(brand_id, nextline);
                    }
                }
                table.put(user_id, oneline);
            }
            for (String key1: table.keySet()) {
                long totalNum = 0;
                double [] clickProb = new double[2];
                double [] carProb = new double[4];
                double [] saveProb = new double[4];
                double [] buyProb = new double[16];

                for (String key2: table.get(key1).keySet()) {
/*                    System.out.print(key1 + " " + key2 + " ");
                    System.out.println(table.get(key1).get(key2));*/
                    Map<Integer, HashSet<Integer>> one = table.get(key1).get(key2);
                    totalNum += one.size();
                    for (Integer i: one.keySet()) {
                        HashSet<Integer> m = one.get(i);
                        
                    }
                }
            }
            br.close();
        }
        catch (Exception e) {
            System.out.println("read file error");
        }
    }
}
