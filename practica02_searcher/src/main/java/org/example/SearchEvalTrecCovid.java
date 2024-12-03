package org.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.example.utils.arguments.Arguments;
import org.example.utils.queries.Query;
import org.example.utils.queries.QueryC;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;

public class SearchEvalTrecCovid {

    public static void main(String[] args) {
        if (args.length != 11) {
            throw new IllegalArgumentException("Usage: SearchEvalTrecCovid -search [jm <lambda> | bm25 k1>] -index <path> -cut <n> -top <m> -queries [all | <int1> | <int1-int2>]");
        }

        Arguments arguments = new Arguments(args, SearchEvalTrecCovid.class.getSimpleName());

        Similarity search = arguments.getSearch();
        Path indexPath = arguments.getIndexPath();
        int cut = arguments.getCut();
        int top = arguments.getTop();
        int explore = max(cut, top);
        String opt = arguments.getQueries();

        try (IndexReader reader = DirectoryReader.open(FSDirectory.open(indexPath))) {

            // test.tsv to map
            List<Map<String, Integer>> maps = QueryC.getRelevance();
            Map<String, Integer> idDocRelv = maps.get(0);
            Map<String, Integer> numRelv = maps.get(1);

            // query list
            List<Query> querys = QueryC.parseQueries(opt);

            QueryParser queryParser = new QueryParser("text", new StandardAnalyzer());

            // Set reader
            IndexSearcher searcher = new IndexSearcher(reader);
            searcher.setSimilarity(search);

            // Init means
            float MPan = 0;
            float MRecallan = 0;
            float MRR = 0;
            float MApan = 0;

            // Get file title
            String sim;
            String metr;
            String val;
            if (search.toString().startsWith("BM25")) {
                sim = "bm25";
                metr = "k1.";
                val = search.toString().substring(8, 11);
            } else {
                sim = "jm";
                metr = "lambda.";
                val = search.toString().substring(18, 21);
            }

            // Set files
            FileWriter TXTfile = new FileWriter("DOCS/TREC-COVID." + sim + "." + top + ".hits." + metr + val + ".q" + opt + ".txt");
            FileWriter CSVfile = new FileWriter("DOCS/TREC-COVID." + sim + "." + cut + ".cut." + metr + val + ".q" + opt + ".csv");
            CSVfile.write("ID,P@" + cut + ",Recall@" + cut + ",RR,AP@" + cut + "\n");


            // Query iterate
            for (Query queryJson : querys) {
                int nRelv = numRelv.get(queryJson.id()) == null ? 0 : numRelv.get(queryJson.id());
                if (nRelv == 0) continue;
                try {
                    // Parse query
                    org.apache.lucene.search.Query query = queryParser.parse(queryJson.query().toLowerCase());

                    // Print query info
                    System.out.println("ID: " + queryJson.id());
                    TXTfile.write("ID: " + queryJson.id() + "\n");
                    System.out.println("Query: " + queryJson.query());
                    TXTfile.write("Query: " + queryJson.query() + "\n\n");

                    // Get scores
                    List<ScoreDoc> scores = List.of(searcher.search(query, explore).scoreDocs);
                    StoredFields getField = searcher.storedFields();

                    List<String> topDocs = new ArrayList<>();
                    float relvInN = 0;
                    float firstRelv = -1;
                    float ap = 0;

                    // Iterate over the scores
                    for (int i = 0; i < scores.size(); i++) {
                        ScoreDoc score = scores.get(i);
                        String id = getField.document(score.doc).getField("id").stringValue();

                        Integer relv = idDocRelv.get(queryJson.id() + id);

                        // Note the relevants
                        if (i < cut) {
                            if (relv != null && relv > 0) {
                                if (relvInN == 0) firstRelv = i;
                                relvInN++;
                                ap = ap + (relvInN / (i + 1));
                            }
                        }

                        // Note top m docs
                        if (topDocs.size() < top) {
                            topDocs.add(
                                    "\tID:    " + id +
                                            "\n\tTitle: " + getField.document(score.doc).getField("title").stringValue() +
                                            "\n\tText:  " + getField.document(score.doc).getField("text").stringValue() +
                                            "\n\tURL:   " + getField.document(score.doc).getField("url").stringValue() +
                                            "\n\tPubId: " + getField.document(score.doc).getField("pubmed_id").stringValue() +
                                            "\n\tScore: " + score.score + "   \t" + (relv != null && relv > 0 ? "*" : " ")
                            );
                        }
                    }

                    for (String doc : topDocs) {
                        System.out.println(doc);
                        System.out.println();
                        TXTfile.write(doc + "\n\n");
                    }

                    // Calculate metrics
                    float Pan = relvInN / cut;
                    float Recallan = relvInN / nRelv;
                    float RR = (float) (1.0 / (firstRelv + 1));
                    ap = ap / nRelv;

                    if (relvInN == 0) {
                        RR = 0;
                    } else {
                        MPan += Pan;
                        MRecallan += Recallan;
                        MRR += RR;
                        MApan += ap;
                    }

                    // Print metrics
                    System.out.println("\tP@" + cut + ": " + Pan);
                    System.out.println("\tRecall@" + cut + ": " + Recallan);
                    System.out.println("\tRR: " + RR);
                    System.out.println("\tAP@" + cut + ": " + ap);
                    System.out.println();
                    TXTfile.write("\tP@" + cut + ": " + Pan + "\n\tRecall@" + cut + ": " + Recallan + "\n\tRR: " + RR + "\n\tAp@" + cut + ": " + ap + "\n\n");
                    CSVfile.write(queryJson.id() + "," + Pan + "," + Recallan + "," + RR + "," + ap + "\n");

                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
            }

            int numQ = querys.size();
            // Calculate mean
            MPan = MPan / numQ;
            MRecallan = MRecallan / numQ;
            MRR = MRR / numQ;
            MApan = MApan / numQ;

            // Print mean
            System.out.println("Mean P@" + cut + ": " + MPan);
            System.out.println("Mean Recall@" + cut + ": " + MRecallan);
            System.out.println("MRR: " + MRR);
            System.out.println("MAP: " + MApan);

            TXTfile.write("Mean P@" + cut + ": " + MPan + "\nMean Recall@" + cut + ": " + MRecallan + "\nMRR: " + MRR + "\nMAP: " + MApan);
            TXTfile.close();
            CSVfile.write("Mean," + MPan + "," + MRecallan + "," + MRR + "," + MApan);
            CSVfile.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
