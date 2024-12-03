package org.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.example.utils.arguments.Arguments;
import org.example.utils.queries.Query;
import org.example.utils.queries.QueryC;
import org.example.utils.trainingtest.TrainingTest;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TrainingTestTrecCovid {

    public static void main(String[] args) {
        if (args.length != 9) {
            throw new IllegalArgumentException("Usage: TrainingTestTrecCovid -[evaljm | evalbm25] [<int1-int2> <int3-int4>] -cut <n> -metrica [P | R | MRR | MAP] -index <path>");
        }

        Arguments arguments = new Arguments(args, TrainingTestTrecCovid.class.getSimpleName());

        String evalType = arguments.getEvalType();  // "-evaljm" | "-evalbm25"
        int[] eval = arguments.getEval();           // [int1, int2, int3, int4]
        int cut = arguments.getCut();
        String metrica = arguments.getMetrica();
        Path indexPath = arguments.getIndexPath();
        Path docsPath = Paths.get("DOCS/");

        double[] arrayLambdas = {0.001f, 0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1.0f};
        double[] arrayK1 = {0.4f, 0.6f, 0.8f, 1.0f, 1.2f, 1.4f, 1.6f, 1.8f, 2.0f};
        Similarity similarity;

        DirectoryReader indexReader = TrainingTest.getIndexReader(indexPath);

        try {
            // test.tsv to map
            List<Map<String, Integer>> maps = QueryC.getRelevance();
            Map<String, Integer> idDocRelv = maps.get(0);
            Map<String, Integer> numRelv = maps.get(1);

            // searcher and parser
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            QueryParser queryParser = new QueryParser("text", new StandardAnalyzer());

            if (evalType.equals("-evaljm")) {
                // TRAINING
                List<Double> lambdas = new ArrayList<>(), meanMetrics = new ArrayList<>();
                List<List<Double>> queryResults = new ArrayList<>();
                Path csvTrainingName = Paths.get(
                        docsPath + "/TREC-COVID.jm.training."
                                + eval[0] + "-" + eval[1] + ".test." + eval[2] + "-" + eval[3] + "."
                                + getMetricName(metrica, cut) + ".training.csv");
                Path csvTestName = Paths.get(
                        docsPath + "/TREC-COVID.jm.training."
                                + eval[0] + "-" + eval[1] + ".test." + eval[2] + "-" + eval[3] + "."
                                + getMetricName(metrica, cut) + ".test.csv");

                // TRAINING
                for (double lambda : arrayLambdas) {
                    // Setting lambda
                    similarity = new LMJelinekMercerSimilarity((float) lambda);
                    indexSearcher.setSimilarity(similarity);

                    // Getting queries for doing the training
                    List<Query> queriesForTrain = QueryC.parseQueries(eval[0] + "-" + eval[1]);

                    // Receiving the results of training
                    List<Double> resultsTrain;
                    resultsTrain = getResults(idDocRelv, numRelv, cut, metrica, indexSearcher, queryParser, queriesForTrain);
                    queryResults.add(resultsTrain);
                    meanMetrics.add(arrayMean(resultsTrain));

                    // Recording this lambda
                    lambdas.add(lambda);
                }

                // Writing the results on a CSV file
                double bestLambda = writeCSVFile(csvTrainingName, metrica, cut, lambdas, queryResults, meanMetrics, eval[0]);
                readCSVForPrinting(csvTrainingName);

                // TEST
                // Getting the queries for testing the best lambda
                List<Query> queriesForTest = QueryC.parseQueries(eval[2] + "-" + eval[3]);

                // Resetting the variables used in the training phase
                List<Double> resultsTest;
                lambdas = new ArrayList<>();
                meanMetrics = new ArrayList<>();
                queryResults = new ArrayList<>();

                // Setting the best lambda
                similarity = new LMJelinekMercerSimilarity((float) bestLambda);
                indexSearcher.setSimilarity(similarity);

                // Getting the results of testing the new queries
                resultsTest = getResults(idDocRelv, numRelv, cut, metrica, indexSearcher, queryParser, queriesForTest);

                lambdas.add(bestLambda);
                queryResults.add(resultsTest);
                meanMetrics.add(arrayMean(resultsTest));

                writeCSVFile(csvTestName, metrica, cut, lambdas, queryResults, meanMetrics, eval[2]);
                readCSVForPrinting(csvTestName);

            } else if (evalType.equals("-evalbm25")) {
                // TRAINING
                List<Double> k1s = new ArrayList<>(), meanMetrics = new ArrayList<>();
                List<List<Double>> queryResults = new ArrayList<>();
                Path csvTrainingName = Paths.get(
                        docsPath + "/TREC-COVID.bm25.training."
                                + eval[0] + "-" + eval[1] + ".test." + eval[2] + "-" + eval[3] + "."
                                + getMetricName(metrica, cut) + ".training.csv");
                Path csvTestName = Paths.get(
                        docsPath + "/TREC-COVID.bm25.training."
                                + eval[0] + "-" + eval[1] + ".test." + eval[2] + "-" + eval[3] + "."
                                + getMetricName(metrica, cut) + ".test.csv");

                for (double k1 : arrayK1) {
                    // Setting lambda
                    similarity = new BM25Similarity((float) k1, 0.75F);
                    indexSearcher.setSimilarity(similarity);

                    // Getting queries for doing the training
                    List<Query> queriesForTrain = QueryC.parseQueries(eval[0] + "-" + eval[1]);

                    // Receiving the results of training
                    List<Double> resultsTrain;
                    resultsTrain = getResults(idDocRelv, numRelv, cut, metrica, indexSearcher, queryParser, queriesForTrain);
                    queryResults.add(resultsTrain);
                    meanMetrics.add(arrayMean(resultsTrain));

                    // Recording this K1 values
                    k1s.add(k1);
                }

                float bestK1 = (float) writeCSVFile(csvTrainingName, metrica, cut, k1s, queryResults, meanMetrics, eval[0]);
                readCSVForPrinting(csvTrainingName);

                // TEST
                // Getting the queries for testing the best K1
                List<Query> queriesForTest = QueryC.parseQueries(eval[2] + "-" + eval[3]);

                // Resetting the variables used in the training phase
                List<Double> resultsTest;
                k1s = new ArrayList<>();
                meanMetrics = new ArrayList<>();
                queryResults = new ArrayList<>();

                // Setting the best k1
                similarity = new BM25Similarity(bestK1, 0.75F);
                indexSearcher.setSimilarity(similarity);

                // Getting the results of testing the new queries
                resultsTest = getResults(idDocRelv, numRelv, cut, metrica, indexSearcher, queryParser, queriesForTest);

                k1s.add((double) bestK1);
                queryResults.add(resultsTest);
                meanMetrics.add(arrayMean(resultsTest));

                writeCSVFile(csvTestName, metrica, cut, k1s, queryResults, meanMetrics, eval[2]);
                readCSVForPrinting(csvTestName);
            } else {
                throw new IllegalArgumentException("Unrecognized Eval type: " + evalType);
            }

        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }


    }

    private static List<Double> getResults(Map<String, Integer> idDocRelv, Map<String, Integer> numRelv, int cut, String metrica, IndexSearcher indexSearcher, QueryParser queryParser, List<Query> queriesForTrain) throws ParseException, IOException {
        List<Double> results = new ArrayList<>();

        for (Query queryFromList : queriesForTrain) {
            int nRelv = numRelv.get(queryFromList.id()) == null ? 0 : numRelv.get(queryFromList.id());
            if(nRelv == 0) continue;
            int firstRelv = -1;
            int relvInN = 0;
            double ap = 0;

            org.apache.lucene.search.Query query = queryParser.parse(queryFromList.query());
            List<ScoreDoc> scores = List.of(indexSearcher.search(query, cut).scoreDocs);

            for (int i = 0; i < scores.size(); i++) {
                ScoreDoc score = scores.get(i);
                String id = indexSearcher.storedFields().document(score.doc).getField("id").stringValue();
                Integer relv = idDocRelv.get(queryFromList.id() + id);

                if (relv != null && relv > 0) {
                    if (relvInN == 0) firstRelv = i;
                    relvInN++;
                    if (Objects.equals(metrica, "MAP")) ap = ap + ((double) relvInN / (i + 1));
                }
            }

            double metricResult = getMetricResult(metrica, cut, firstRelv, nRelv, relvInN, ap);
            results.add(metricResult);
        }

        return results;
    }

    private static double getMetricResult(String metric, int cut, int firstRelv, int nRelv, int relvInN, double partialP) {
        return switch (metric) {
            case "P" -> (double) relvInN / cut;
            case "R" -> (double) relvInN / nRelv;
            case "MRR" -> relvInN == 0 ? 0 : (double) 1 / (firstRelv + 1);
            case "MAP" -> partialP / nRelv;
            default -> throw new IllegalStateException("Unexpected value: " + metric);
        };

    }

    private static double arrayMean(List<Double> array) {
        double sum = 0;

        for (double element : array) {
            sum += element;
        }

        return sum / array.size();
    }

    private static double getBestMean(List<Double> meanMetrics) {
        double maximum = Double.MIN_VALUE;

        for (double mean : meanMetrics) {
            if (mean > maximum) {
                maximum = mean;
            }
        }

        return maximum;
    }

    private static String getMetricName(String metrica, int cut) {
        return switch (metrica) {
            case "P" -> "p" + cut;
            case "R" -> "r" + cut;
            case "MRR" -> "mrr";
            case "MAP" -> "map" + cut;
            default -> throw new IllegalStateException("Unexpected value: " + metrica);
        };
    }

    private static double writeCSVFile(Path path, String metrica, int cut, List<Double> lambdas,
                                      List<List<Double>> queriesResults, List<Double> meanMetrics, int idFirstQuery) {

        String firstLine = getString(getMetricName(metrica, cut), lambdas);
        String results = queryMetricToString(queriesResults, idFirstQuery);
        String lastLine = getString("Mean", meanMetrics);

        try {
            FileWriter CSVWriter = new FileWriter(path.toFile());
            CSVWriter.write(firstLine + "\n");
            CSVWriter.write(results);
            CSVWriter.write(lastLine);
            CSVWriter.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return lambdas.get(meanMetrics.indexOf(getBestMean(meanMetrics)));
    }

    private static String getString(String firstColumn, List<Double> numbers) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;

        builder.append(firstColumn).append(",");

        for (Double element : numbers) {
            if (!first) {
                builder.append(",");
            }
            builder.append(element);
            first = false;
        }

        return builder.toString();
    }

    private static String queryMetricToString(List<List<Double>> queriesResults, int idFirstQuery) {
        StringBuilder builder = new StringBuilder();
        double[][] array = new double[queriesResults.get(0).size()][queriesResults.size()];
        int i = 0, j = 0;

        for (List<Double> query : queriesResults) {
            for (double result : query) {
                array[i][j] = result;
                i++;
            }
            i = 0;
            j++;
        }

        int firstTest = idFirstQuery;

        for (double[] doubles : array) {
            boolean first = true;
            builder.append(firstTest++).append(",");
            for (double aDouble : doubles) {
                if (!first) {
                    builder.append(",");
                }
                builder.append(aDouble);
                first = false;
            }
            builder.append("\n");
        }

        return builder.toString();
    }

    private static void readCSVForPrinting(Path filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename.toFile()));
            String line;
            while((line = br.readLine()) != null) {
                System.out.println(line.replaceAll(",", "\t"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("\n\n");
    }
}
