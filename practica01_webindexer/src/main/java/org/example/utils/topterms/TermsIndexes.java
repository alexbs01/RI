package org.example.utils.topterms;

import java.util.Comparator;
import java.util.List;

public class TermsIndexes {
    String term;
    long termFrequency;
    int docFreq;
    double idf;
    double relevance;
    int docID;

    public TermsIndexes(String term, long termFrquency, int docFreq, double idf, double relevance, int docID) {
        this.term = term;
        this.termFrequency = termFrquency;
        this.docFreq = docFreq;
        this.idf = idf;
        this.relevance = relevance;
        this.docID = docID;
    }

    /**
     * Sort a list by the Relevance attribute
     *
     * @param list -> List sort
     */
    public static void sortByRelevance(List<TermsIndexes> list) {
        list.sort((o1, o2) -> Float.compare((float) o2.getRelevance(), (float) o1.getRelevance()));
    }

    public static void sortByDF(List<TermsIndexes> list) {
        list.sort(Comparator.comparingInt(TermsIndexes::getDocFreq));
        revlist(list);
    }
    private static <T> void revlist(List<T> list) {
        if (list == null || list.size() <= 1) return;
        T value = list.remove(0);
        revlist(list);
        list.add(value);
    }


    public long getTermFrequency() {
        return termFrequency;
    }

    public String getTerm() {
        return term;
    }

    public int getDocFreq() {
        return docFreq;
    }

    public double getIdf() {
        return idf;
    }

    public double getRelevance() {
        return relevance;
    }

    public int getDocID() {
        return docID;
    }
}
