package com.alibaba;

import org.eclipse.recommenders.jayes.BayesNet;
import org.eclipse.recommenders.jayes.BayesNode;
import org.eclipse.recommenders.jayes.inference.junctionTree.JunctionTreeAlgorithm;
import org.eclipse.recommenders.jayes.util.NumericalInstabilityException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * User: sweeliao@gmail.com
 * Date: 14-3-20
 * Time: 下午4:26
 */
public class TrainV2 {
    private static double [] clickProb = new double[2];
    private static double [] carProb = new double[4];
    private static double [] saveProb = new double[4];
    private static double [] buyProb = new double[16];
    private static JunctionTreeAlgorithm inferer = new JunctionTreeAlgorithm();
    private static BayesNode a, b, c, d;
    private static BayesNet net;

    public static void createNetwork() {
        net = new BayesNet();
        a = net.createNode("a");
        a.addOutcomes("true", "false");


        b = net.createNode("b");
        b.addOutcomes("true", "false");
        b.setParents(Arrays.asList(a));


        c = net.createNode("c");
        c.addOutcomes("true", "false");
        c.setParents(Arrays.asList(a));


        d = net.createNode("d");
        d.addOutcomes("true", "false");
        d.setParents(Arrays.asList(a, b, c));

        inferer.getFactory().setUseLogScale(true);
        inferer.getFactory().setFloatingPointType(float.class);
    }

    public static void initNetwork() {
        a.setProbabilities(clickProb);
        b.setProbabilities(saveProb);
        c.setProbabilities(carProb);
        d.setProbabilities(buyProb);


        inferer.setNetwork(net);
    }

    public static void setEvidence(Set<Integer> s) {
        Map<BayesNode,String> evidence = new HashMap<BayesNode,String>();
        for (Integer i: s) {
            if (i == 0) evidence.put(a, "true");
            else if (i == 2) evidence.put(b, "true");
            else if (i == 3) evidence.put(c, "true");
        }
        inferer.setEvidence(evidence);
    }

    public static Boolean isExpendable() {
        try {
            double [] belifes = inferer.getBeliefs(d);
/*            System.out.println(belifes[0] + " " + belifes[1]);*/
            return belifes[0] > belifes[1];
        }
        catch (NumericalInstabilityException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            BufferedReader br = new BufferedReader(new FileReader("E:\\workspace\\alibaba\\t_alibaba_data.csv"));
            String line;
            Map<String, Map<String, Map<Integer, HashSet<Integer>>>> table = new HashMap<String, Map<String, Map<Integer, HashSet<Integer>>>>();
            createNetwork();
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] stat = line.split(",");
                String [] date = stat[3].split("\\W+");
                String user_id = stat[0],
                        brand_id = stat[1],
                        type = stat[2];

                //Integer datehash = Integer.valueOf(date[0]) * 31 + Integer.valueOf(date[1]); // result number is 0
                //Integer datehash = Integer.valueOf(date[0]); // result number is 2
                Integer datehash = 1;  // result number is 1098

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

            long totalSum = 0;

            Set<String> allSet = new HashSet<String>();
            Set<String> clickSet = new HashSet<String>();
            Set<String> carSet = new HashSet<String>();
            Set<String> saveSet = new HashSet<String>();
            Set<String> buySet = new HashSet<String>();

            for (String key1: table.keySet()) {
                for (String key2: table.get(key1).keySet()) {
                    Map<Integer, HashSet<Integer>> one = table.get(key1).get(key2);
                    for (Integer i: one.keySet()) {
                        HashSet<Integer> m = one.get(i);
                        for (Integer n: m) {
                            String index = key2;
                            allSet.add(index);
                            if (n == 0) clickSet.add(index);
                            else if (n == 1) buySet.add(index);
                            else if (n == 2) saveSet.add(index);
                            else if (n == 3) carSet.add(index);
                        }
                    }
                }
            }

            double beforeProbability = 0;
            double unionProb = 0;
            // calculate probability of click
            // BayesNode a = net.createNode("a");点
            // a.addOutcomes("true", "false");
            clickProb[0] = clickSet.size() / (double)allSet.size();
            clickProb[1] = 1 - clickProb[0];

            // calculate condition probability of save
            // BayesNode b = net.createNode("b");收
            // b.addOutcomes("true", "false");
            // b.setParents(Arrays.asList(a));
            Set<String> intersection = new HashSet<String>(clickSet);
            beforeProbability = intersection.size();

            intersection.retainAll(saveSet);
            unionProb = intersection.size();
            saveProb[0] = unionProb / beforeProbability;
            saveProb[1] = 1 - saveProb[0];

            Set<String> notClickSet = new HashSet<String>(allSet);
            notClickSet.removeAll(clickSet);
            intersection = new HashSet<String>(notClickSet);
            beforeProbability = intersection.size();
            if (beforeProbability == 0) {
                saveProb[0] = 0;
                saveProb[1] = 1;
            }
            else {
                intersection.retainAll(saveSet);
                unionProb = intersection.size();
                saveProb[2] = (unionProb) / beforeProbability;
                saveProb[3] = 1 - saveProb[2];
            }

            // calculate condition probability of car
            // BayesNode c = net.createNode("c");车
            // c.addOutcomes("true", "false");
            // c.setParents(Arrays.asList(a);
            intersection = new HashSet<String>(clickSet);
            beforeProbability = intersection.size();
            intersection.retainAll(carSet);
            unionProb = intersection.size();
            carProb[0] = unionProb / beforeProbability;
            carProb[1] = 1 - carProb[0];

            intersection = new HashSet<String>(notClickSet);
            beforeProbability = intersection.size();
            if (beforeProbability == 0) {
                carProb[2] = 0;
                carProb[3] = 1;
            }
            else {
                intersection.retainAll(carSet);
                unionProb = intersection.size();
                carProb[2] = unionProb / beforeProbability;
                carProb[3] = 1 - carProb[2];
            }

            // calculate condition probability of buy
            // BayesNode d = net.createNode("d");买
            // d.addOutcomes("true", "false");
            // c.setParents(Arrays.asList(a, b, c);
            intersection = new HashSet<String>(clickSet);
            intersection.retainAll(saveSet);
            intersection.retainAll(carSet);
            beforeProbability = intersection.size();
            if (beforeProbability == 0) {
                buyProb[0] = 0.8;
                buyProb[1] = 0.2;
            }
            else {
                intersection.retainAll(buySet);
                unionProb = intersection.size();
                buyProb[0] = unionProb / beforeProbability;
                buyProb[1] = 1 - buyProb[0];
            }

            intersection = new HashSet<String>(clickSet);
            intersection.retainAll(saveSet);
            Set<String> notCarSet = new HashSet<String>(allSet);
            notCarSet.removeAll(carSet);
            intersection.retainAll(notCarSet);
            beforeProbability = intersection.size();
            if (beforeProbability == 0) {
                buyProb[2] = 0.6;
                buyProb[3] = 0.4;
            }
            else {
                intersection.retainAll(buySet);
                unionProb = intersection.size();
                buyProb[2] = unionProb / beforeProbability;
                buyProb[3] = 1 - buyProb[2];
            }

            intersection = new HashSet<String>(clickSet);
            Set<String> notSaveSet = new HashSet<String>(allSet);
            notSaveSet.removeAll(saveSet);
            intersection.retainAll(notSaveSet);
            intersection.retainAll(carSet);
            beforeProbability = intersection.size();
            if (beforeProbability == 0) {
                buyProb[4] = 0.6;
                buyProb[5] = 0.4;
            }
            else {
                intersection.retainAll(buySet);
                unionProb = intersection.size();
                buyProb[4] = unionProb / beforeProbability;
                buyProb[5] = 1 - buyProb[4];
            }

            intersection = new HashSet<String>(clickSet);
            intersection.retainAll(notSaveSet);
            intersection.retainAll(notCarSet);
            beforeProbability = intersection.size();
            if (beforeProbability == 0) {
                buyProb[6] = 0.5;
                buyProb[7] = 0.5;
            }
            else {
                intersection.retainAll(buySet);
                unionProb = intersection.size();
                buyProb[6] = unionProb / beforeProbability;
                buyProb[7] = 1 - buyProb[6];
            }

            intersection = new HashSet<String>(notClickSet);
            intersection.retainAll(saveSet);
            intersection.retainAll(carSet);
            beforeProbability = intersection.size();
            if (beforeProbability == 0) {
                buyProb[8] = 0.6;
                buyProb[9] = 0.4;
            }
            else {
                intersection.retainAll(buySet);
                unionProb = intersection.size();
                buyProb[8] = unionProb / beforeProbability;
                buyProb[9] = 1 - buyProb[8];
            }

            intersection = new HashSet<String>(notClickSet);
            intersection.retainAll(saveSet);
            intersection.retainAll(notCarSet);
            beforeProbability = intersection.size();
            if (beforeProbability == 0) {
                buyProb[10] = 0.5;
                buyProb[11] = 0.5;
            }
            else {
                intersection.retainAll(buySet);
                unionProb = intersection.size();
                buyProb[10] = unionProb / beforeProbability;
                buyProb[11] = 1 - buyProb[10];
            }

            intersection = new HashSet<String>(notClickSet);
            intersection.retainAll(notSaveSet);
            intersection.retainAll(carSet);
            beforeProbability = intersection.size();
            if (beforeProbability == 0) {
                buyProb[12] = 0.5;
                buyProb[13] = 0.5;
            }
            else {
                intersection.retainAll(buySet);
                unionProb = intersection.size();
                buyProb[12] = unionProb / beforeProbability;
                buyProb[13] = 1 - buyProb[12];
            }

            intersection = new HashSet<String>(notClickSet);
            intersection.retainAll(notSaveSet);
            intersection.retainAll(notCarSet);
            beforeProbability = intersection.size();
            if (beforeProbability == 0) {
                buyProb[14] = 0.2;
                buyProb[15] = 0.8;
            }
            else {
                intersection.retainAll(buySet);
                unionProb = intersection.size();
                buyProb[14] = unionProb / beforeProbability;
                buyProb[15] = 1 - buyProb[14];
            }

            initNetwork();

            for (String key1: table.keySet()) {
                for (String key2: table.get(key1).keySet()) {
                    Map<Integer, HashSet<Integer>> one = table.get(key1).get(key2);
                    int maxValueInMap=(Collections.max(one.keySet()));
                    Set<Integer> res = one.get(maxValueInMap);
                    res.remove(1);
                    setEvidence(res);
                    if (isExpendable()) {
                        System.out.println("for user id " + key1);
                        System.out.println(key2);
                        totalSum++;
                    }
                }
            }
            System.out.println("total brand numbers is " + totalSum);
            br.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
