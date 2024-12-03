package org.example;

import org.apache.commons.math3.stat.inference.TTest;
import org.apache.commons.math3.stat.inference.WilcoxonSignedRankTest;
import org.example.utils.arguments.Arguments;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Compare {
    public static void main(String[] args) {
        if (args.length != 5 && args.length != 6) {
            throw new IllegalArgumentException("Usage: Compare -test [t | wilcoxon <alpha>] -results <results1.csv> <results2.csv>");
        }

        Arguments arguments = new Arguments(args, Compare.class.getSimpleName());
        float alpha = arguments.getAlpha();
        String significanceTest = arguments.getSignificanceTest();
        Path[] results = arguments.getResults();
        double[] results1 = getResultsToDouble(results[0]);
        double[] results2 = getResultsToDouble(results[1]);

        double pValue;


        if (significanceTest.equals("t")) {
            TTest tTest = new TTest();

            pValue = tTest.pairedTTest(results1, results2);

        } else {
            WilcoxonSignedRankTest wilcoxonSignedRankTest = new WilcoxonSignedRankTest();

            pValue = wilcoxonSignedRankTest.wilcoxonSignedRankTest(results1, results2, false);
        }

        if(pValue < alpha)
            System.out.println("Significant difference, (pValue < alpha) : " + "(" + pValue + " < " + alpha + ")");
        else
            System.out.println("No significant difference, (pValue > alpha) : " + "(" + pValue + " > " + alpha + ")");
    }

    private static List<Double> getResultsFromCSV(Path results) {
        List<Double> datas = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new FileReader(results.toFile()));
            String line;

            while((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                datas.add(Double.parseDouble(values[1]));
            }

            datas.remove(0);
            datas.remove(datas.size() - 1);

            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return datas;
    }

    private static double[] getResultsToDouble(Path results) {
        List<Double> datas = getResultsFromCSV(results);
        double[] result = new double[datas.size()];
        for (int i = 0; i < datas.size(); i++) {
            result[i] = datas.get(i);
        }

        return result;
    }
}