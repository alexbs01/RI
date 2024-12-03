package org.example.utils.arguments;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

import java.nio.file.Path;
import java.nio.file.Paths;

public class Arguments {
    // Common atributes
    Path indexPath = Paths.get("index");
    Path docsPath = null;
    boolean create = false;
    int numCores = Runtime.getRuntime().availableProcessors();
    boolean threadInformation = false; // "Hilo xxx comienzo url yyy" --> "Hilo xxx fin url yyy"
    boolean aplicationInformation = false; // "Creado índice zzz en mmm ms"
    boolean titleTermVectors = false; // The field title must store Term Vectors
    boolean bodyTermVectors = false; // The field body must store Term Vectors
    Analyzer analyzer = new StandardAnalyzer(); // Select one Analyzer from Lucene

    // TopTerm attributes
    String field = null;
    int topN = 3;
    Path outfilePath = null;

    // TopTermInDoc
    int docId = -1;

    public Arguments(String[] args, String className) {
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-index":
                    this.indexPath = Paths.get(args[++i]);
                    if (!this.indexPath.toFile().exists())
                        throw new IllegalArgumentException("Path of the argument -index does not exist");
                    break;

                case "-docs":
                    illegalArgumentTopTermsInDoc(args[i], className);
                    illegalArgumentTopTermsInField(args[i], className);
                    this.docsPath = Paths.get(args[++i]);
                    if (!this.docsPath.toFile().exists())
                        throw new IllegalArgumentException("Path of the argument -docs does not exist");
                    break;

                case "-create":
                    illegalArgumentTopTermsInDoc(args[i], className);
                    illegalArgumentTopTermsInField(args[i], className);
                    this.create = true;
                    break;

                case "-numThreads":
                    illegalArgumentTopTermsInDoc(args[i], className);
                    illegalArgumentTopTermsInField(args[i], className);
                    this.numCores = Integer.parseInt(args[++i]);
                    break;

                case "-h":
                    illegalArgumentTopTermsInDoc(args[i], className);
                    illegalArgumentTopTermsInField(args[i], className);
                    this.threadInformation = true;
                    break;

                case "-p":
                    illegalArgumentTopTermsInDoc(args[i], className);
                    illegalArgumentTopTermsInField(args[i], className);
                    this.aplicationInformation = true;
                    break;

                case "-titleTermVectors":
                    illegalArgumentTopTermsInDoc(args[i], className);
                    illegalArgumentTopTermsInField(args[i], className);
                    this.titleTermVectors = true;
                    break;

                case "-bodyTermVectors":
                    illegalArgumentTopTermsInDoc(args[i], className);
                    illegalArgumentTopTermsInField(args[i], className);
                    this.bodyTermVectors = true;
                    break;

                case "-analyzer":
                    illegalArgumentTopTermsInDoc(args[i], className);
                    illegalArgumentTopTermsInField(args[i], className);
                    this.analyzer = new GetAnalyzer(args[++i]).get();
                    break;

                case "-field":
                    illegalArgumentWebIndexer(args[i], className);
                    this.field = args[++i];
                    break;

                case "-docID":
                    illegalArgumentWebIndexer(args[i], className);
                    illegalArgumentTopTermsInField(args[i], className);
                    this.docId = Integer.parseInt(args[++i]);
                    break;

                case "-top":
                    illegalArgumentWebIndexer(args[i], className);
                    this.topN = Integer.parseInt(args[++i]);
                    break;

                case "-outfile":
                    illegalArgumentWebIndexer(args[i], className);
                    this.outfilePath = Paths.get(args[++i]);
                    break;

                default:
                    throw new IllegalArgumentException("unknown parameter " + args[i]);
            }
        }
    }

    private void illegalArgumentWebIndexer(String arg, String className) {
        if (className.equals("WebIndexer")) {
            throw new IllegalArgumentException(arg);
        }
    }

    private void illegalArgumentTopTermsInDoc(String arg, String className) {
        /*
        System.out.println(getClass().getSimpleName());
        System.out.println(getClass().getSimpleName().equals("TopTermsInDoc"));
        */
        if (className.equals("TopTermsInDoc")) {
            throw new IllegalArgumentException(arg);
        }
    }

    private void illegalArgumentTopTermsInField(String arg, String className) {
        if (className.equals("TopTermsInField")) {
            throw new IllegalArgumentException(arg);
        }
    }

    public Path getIndexPath() {
        return indexPath;
    }

    public Path getDocsPath() {
        return docsPath;
    }

    public boolean isCreate() {
        return create;
    }

    public int getNumCores() {
        return numCores;
    }

    public boolean isThreadInformation() {
        return threadInformation;
    }

    public boolean isAplicationInformation() {
        return aplicationInformation;
    }

    public boolean isTitleTermVectors() {
        return titleTermVectors;
    }

    public boolean isBodyTermVectors() {
        return bodyTermVectors;
    }

    public Analyzer getAnalyzer() {
        return analyzer;
    }

    public String getField() {
        return field;
    }

    public int getTopN() {
        return topN;
    }

    public Path getOutfilePath() {
        return outfilePath;
    }

    public int getDocId() {
        return docId;
    }
}
