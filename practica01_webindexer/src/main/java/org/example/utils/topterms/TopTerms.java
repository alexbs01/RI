package org.example.utils.topterms;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.FSDirectory;
import org.example.utils.arguments.Arguments;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class TopTerms {
    public static DirectoryReader getIndexReader(Path index) {
        FSDirectory directory;
        DirectoryReader indexReader = null;

        try {
            directory = FSDirectory.open(index);
            indexReader = DirectoryReader.open(directory);
        } catch (IOException e) {
            System.out.println("Index collect error");
            e.printStackTrace();
            System.exit(1);
        }

        return indexReader;
    }

    public static TermsIndexes getTermToList(Arguments arguments, TermsEnum termsEnum, DirectoryReader indexReader, int docid) throws IOException {
        String termString = termsEnum.term().utf8ToString();
        Term term = new Term(arguments.getField(), termString);
        long termFrequency = termsEnum.totalTermFreq();
        int docFreq = indexReader.docFreq(term);

        //float idf = tfidf.idf(docFreq, numDoc);
        double idf = Math.log10((double) indexReader.numDocs() / (docFreq + 1));
        double relevance = termFrequency * idf;

        return new TermsIndexes(termString, termFrequency, docFreq, idf, relevance, docid);
    }

    public static void showResults(int top, String field, Arguments arguments, List<TermsIndexes> termsIndexes, FieldInfo fieldinfo) {
        List<String> save = new ArrayList<>();

        save.add("Top " + top + (arguments.getDocId() != -1 ? " terms in document " + arguments.getDocId() : "") + " in field " + field);

        // formatting
        int[] lengths = {Math.max(3, Integer.toString(top).length()), 4, 5, Math.max(5, fieldinfo.name.length()), 2, 2, 13};

        for (int i = 0; i < top && i < termsIndexes.size(); i++) {
            lengths[1] = Math.max(lengths[1], termsIndexes.get(i).getTerm().length());
            lengths[2] = Math.max(lengths[2], Integer.toString(termsIndexes.get(i).getDocID()).length());
            lengths[4] = Math.max(lengths[4], Long.toString(termsIndexes.get(i).getTermFrequency()).length());
            lengths[5] = Math.max(lengths[5], Long.toString(termsIndexes.get(i).getDocFreq()).length());
            lengths[6] = Math.max(lengths[6], Double.toString(termsIndexes.get(i).getRelevance()).length());
        }

        for (int i = 0; i < lengths.length; i++) {
            lengths[i] = lengths[i] + 3;
        }

        String format =
                "%-" + lengths[0] +
                        "s%-" + lengths[1] +
                        (arguments.getDocId()==-1? "s%-" :"s%-" + lengths[2] + "s%-")
                        + lengths[3] +
                        "s%-" + lengths[4] +
                        "s%-" + lengths[5] +
                        "s%-" + lengths[6] + "s";

        if(arguments.getDocId()==-1) {
            save.add(String.format(format,
                    "TOP",
                    "TERM",
                    "FIELD",
                    "TF",
                    "DF",
                    "TF x IDFlog10"
            ));
        }else {
            save.add(String.format(format,
                    "TOP",
                    "TERM",
                    "DOCID",
                    "FIELD",
                    "TF",
                    "DF",
                    "TF x IDFlog10"
            ));
        }


        // print
        if(arguments.getDocId()==-1) {
            for (int i = 0; i < top && i < termsIndexes.size(); i++) {
                save.add(String.format(format,
                        i + 1,
                        termsIndexes.get(i).getTerm(),
                        fieldinfo.name,
                        termsIndexes.get(i).getTermFrequency(),
                        termsIndexes.get(i).getDocFreq(),
                        termsIndexes.get(i).getRelevance()
                ));
            }
        }else {
        for (int i = 0; i < top && i < termsIndexes.size(); i++) {
            save.add(String.format(format,
                    i + 1,
                    termsIndexes.get(i).getTerm(),
                    termsIndexes.get(i).getDocID(),
                    fieldinfo.name,
                    termsIndexes.get(i).getTermFrequency(),
                    termsIndexes.get(i).getDocFreq(),
                    termsIndexes.get(i).getRelevance()
            ));
        }
        }

        save.forEach(System.out::println);

        // save
        if (arguments.getOutfilePath() != null) {
            try (final BufferedWriter writer = Files.newBufferedWriter(arguments.getOutfilePath(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE)) {

                for (String line : save) {
                    writer.write(line);
                    writer.newLine();
                }
                writer.flush();

                System.out.println("Saved on " + arguments.getOutfilePath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
