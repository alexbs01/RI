package org.example.utils.arguments;

import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Arguments {
    IndexWriterConfig.OpenMode openMode;    // IndexTrec
    Path indexPath;                         // IndexTrec, SearchEval, TrainingTest
    Path docsPath;                          // IndexTrec
    Similarity similarity;                  // IndexTrec, SearchEval
    int cut;                                //            SearchEval, TrainingTest
    int top = 3;                            //            SearchEval
    String queries;                         //            SearchEval
    int[] eval = new int[4];                //                        TrainingTest
    String evalType;                        //                        TrainingTest
    int countEval = 0;                      // Auxiliar of eval
    String metrica;                         //                        TrainingTest
    String significanceTest;                //                                      Compare
    float alpha;                            //                                      Compare
    Path[] results = new Path[2];           //                                      Compare

    public Arguments(String[] args, String className) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-openmode":
                    illegalArgumentSearchEvalTrecCovid(args[i], className);
                    illegalArgumentTrainingTestTrecCovid(args[i], className);
                    illegalArgumentCompare(args[i], className);
                    this.openMode = IndexWriterConfig.OpenMode.valueOf(args[++i]);
                    if (!isValidOpenMode(this.openMode)) {
                        throw new IllegalArgumentException("Invalid OpenMode: " + this.openMode + ". Must be 'APPEND', 'CREATE' or 'CREATE_OR_APPEND'");
                    }
                    break;

                case "-index":
                    illegalArgumentCompare(args[i], className);
                    this.indexPath = Paths.get(args[++i]);
                    if (!this.indexPath.toFile().exists())
                        throw new IllegalArgumentException("The path" + this.indexPath + " in the argument -index does not exist");
                    break;

                case "-docs":
                    illegalArgumentSearchEvalTrecCovid(args[i], className);
                    illegalArgumentTrainingTestTrecCovid(args[i], className);
                    illegalArgumentCompare(args[i], className);
                    this.docsPath = Paths.get(args[++i]);
                    if (!this.docsPath.toFile().exists())
                        throw new IllegalArgumentException("The path" + this.docsPath + " in the argument -docs does not exist");
                    break;

                case "-indexingmodel", "-search":
                    if (args[i].equals("-search")) {
                        illegalArgumentIndexTrecCovid(args[i], className);
                    } else {
                        illegalArgumentSearchEvalTrecCovid(args[i], className);
                    }

                    illegalArgumentTrainingTestTrecCovid(args[i], className);
                    illegalArgumentCompare(args[i], className);

                    ++i;
                    if (args[i].equals("jm")) {
                        this.similarity = new LMJelinekMercerSimilarity(Float.parseFloat(args[++i]));

                    } else if (args[i].equals("bm25")) {
                        this.similarity = new BM25Similarity(Float.parseFloat(args[++i]), 0.75f);

                    } else {
                        throw new IllegalArgumentException("Invalid indexing model: " + args[i] + ". Must be 'jm' or 'bm25'");
                    }
                    break;

                case "-cut":
                    illegalArgumentIndexTrecCovid(args[i], className);
                    illegalArgumentCompare(args[i], className);

                    this.cut = Integer.parseInt(args[++i]);
                    break;

                case "-top":
                    illegalArgumentIndexTrecCovid(args[i], className);
                    illegalArgumentTrainingTestTrecCovid(args[i], className);
                    illegalArgumentCompare(args[i], className);

                    this.top = Integer.parseInt(args[++i]);
                    break;

                case "-queries":
                    illegalArgumentIndexTrecCovid(args[i], className);
                    illegalArgumentTrainingTestTrecCovid(args[i], className);
                    illegalArgumentCompare(args[i], className);

                    if (args[++i].equals("all") || isInteger(args[i]) || isIntervalOfTwoIntegers(args[i])) {
                        this.queries = args[i];
                    }
                    break;

                case "-evaljm", "-evalbm25":
                    illegalArgumentIndexTrecCovid(args[i], className);
                    illegalArgumentSearchEvalTrecCovid(args[i], className);
                    illegalArgumentCompare(args[i], className);

                    if (++countEval == 2)
                        throw new IllegalArgumentException("'-evaljm' and 'evalbm25' can not be in the same execution");

                    if (!(isIntervalOfTwoIntegers(args[++i]) && isIntervalOfTwoIntegers(args[++i]))) {
                        throw new IllegalArgumentException("Argument of 'evaljm' or 'evalbm25' is not a valid interval");
                    }

                    String[] int3int4 = args[i].split("-");
                    String[] int1int2 = args[--i].split("-");

                    this.evalType = args[--i];
                    this.eval[0] = Integer.parseInt(int1int2[0]);
                    this.eval[1] = Integer.parseInt(int1int2[1]);
                    this.eval[2] = Integer.parseInt(int3int4[0]);
                    this.eval[3] = Integer.parseInt(int3int4[1]);

                    i += 2;
                    break;

                case "-metrica":
                    illegalArgumentIndexTrecCovid(args[i], className);
                    illegalArgumentSearchEvalTrecCovid(args[i], className);
                    illegalArgumentCompare(args[i], className);

                    if (args[++i].equals("P") || args[i].equals("R") || args[i].equals("MRR") || args[i].equals("MAP")) {
                        this.metrica = args[i];
                    }
                    break;

                case "-test":
                    illegalArgumentIndexTrecCovid(args[i], className);
                    illegalArgumentSearchEvalTrecCovid(args[i], className);
                    illegalArgumentTrainingTestTrecCovid(args[i], className);

                    if(args[++i].equals("t") || args[i].equals("wilcoxon")) {
                        this.significanceTest = args[i];
                        this.alpha = Float.parseFloat(args[++i]);
                    }

                    break;

                case "-results":
                    illegalArgumentIndexTrecCovid(args[i], className);
                    illegalArgumentSearchEvalTrecCovid(args[i], className);
                    illegalArgumentTrainingTestTrecCovid(args[i], className);

                    this.results[0] = Paths.get(args[++i]);
                    this.results[1] = Paths.get(args[++i]);

                    break;

                default:
                    throw new IllegalArgumentException("unknown parameter " + args[i]);
            }
        }
    }

    private boolean isValidOpenMode(IndexWriterConfig.OpenMode openMode) {
        switch (openMode) {
            case CREATE, APPEND, CREATE_OR_APPEND -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private boolean isInteger(String arg) {
        Pattern patternOneInteger = Pattern.compile("^[-+]?\\d+$");
        Matcher matcherOneInteger = patternOneInteger.matcher(arg);
        return matcherOneInteger.matches();
    }

    private boolean isIntervalOfTwoIntegers(String arg) {
        Pattern patternIntervalOfTwoIntegers = Pattern.compile("^\\d+-\\d+$");
        Matcher matcherIntervalOfTwoIntegers = patternIntervalOfTwoIntegers.matcher(arg);
        return matcherIntervalOfTwoIntegers.matches();
    }

    private void illegalArgumentIndexTrecCovid(String arg, String className) {
        if (className.equals("TrecCovid")) {
            throw new IllegalArgumentException("Illegal argument " + arg + "\n" +
                    "Usage: IndexTrecCovid -openmode [APPEND | CREATE | CREATE_OR_APPEND] -index <path> -docs <path> -indexingmodel [jm <lambda> | bm25 <k1>]");
        }
    }

    private void illegalArgumentSearchEvalTrecCovid(String arg, String className) {
        if (className.equals("SearchEvalTrecCovid")) {
            throw new IllegalArgumentException("Illegal argument " + arg + "\n" +
                    "Usage: SearchEvalTrecCovid -search [jm <lambda> | bm25 k1>] -index <path> -cut <n> -top <m> -queries [all | <int1> | <int1-int2>]");
        }
    }

    private void illegalArgumentTrainingTestTrecCovid(String arg, String className) {
        if (className.equals("TrainingTestTrecCovid")) {
            throw new IllegalArgumentException("Illegal argument " + arg + "\n" +
                    "Usage: TrainingTestTrecCovid -[evaljm | evalbm25] [<int1-int2> <int3-int4>] -cut <n> -metrica [P | R | MRR | MAP] -index <path>");
        }
    }

    private void illegalArgumentCompare(String arg, String className) {
        if (className.equals("Compare")) {
            throw new IllegalArgumentException("Illegal argument " + arg + "\n" +
                    "Usage: Compare -test [t | wilcoxon <alpha>] -results <results1.csv> <results2.csv>");
        }
    }

    public IndexWriterConfig.OpenMode getOpenMode() {
        return openMode;
    }

    public Path getIndexPath() {
        return indexPath;
    }

    public Path getDocsPath() {
        return docsPath;
    }

    public Similarity getSearch() {
        return similarity;
    }

    public int getCut() {
        return cut;
    }

    public int getTop() {
        return top;
    }

    public String getQueries() {
        return queries;
    }

    public int[] getEval() {
        return eval;
    }

    public String getEvalType() {
        return evalType;
    }

    public String getMetrica() {
        return metrica;
    }

    public String getSignificanceTest() {
        return significanceTest;
    }

    public float getAlpha() {
        return alpha;
    }

    public Path[] getResults() {
        return results;
    }
}
