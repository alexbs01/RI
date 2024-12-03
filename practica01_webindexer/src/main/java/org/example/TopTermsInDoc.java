package org.example;

import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import org.example.utils.topterms.TermsIndexes;
import org.example.utils.arguments.Arguments;
import org.example.utils.topterms.TopTerms;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TopTermsInDoc {
    public static void main(String[] args) {
        // Check minimum amount of arguments
        if (args.length < 6)
            throw new IllegalArgumentException("Usage: java TopTermsInDoc -index <path> -field <field> -docID <Integer> [-top <n>] [-outfile <path>]");

        // Get arguments 
        Arguments arguments = new Arguments(args, TopTermsInDoc.class.getSimpleName());

        // local arguments
        Path index = arguments.getIndexPath();
        String field = arguments.getField();
        int docID = arguments.getDocId();
        int top = arguments.getTopN();

        DirectoryReader indexReader = TopTerms.getIndexReader(index);

        // Field info
        final FieldInfos fieldinfos = FieldInfos.getMergedFieldInfos(indexReader);
        int numDoc = indexReader.numDocs();
        if (numDoc <= docID)
            throw new IllegalArgumentException("Document ID out of bounds. (Num docs: " + numDoc + ", DocID introduced: " + arguments.getDocId());

        // Terms of a field
        try {
            boolean fieldFound = false;
            // Terms of a field
            for (final FieldInfo fieldinfo : fieldinfos) {
                if (!fieldinfo.name.equals(field)) continue; // Filter only the desired field
                fieldFound = true;

                final Terms terms = MultiTerms.getTerms(indexReader, field);

                if (terms == null) continue; // Continue if no terms
                final TermsEnum termsEnum = terms.iterator();

                List<TermsIndexes> termsIndexes = new ArrayList<>();

                while (termsEnum.next() != null) {
                    String termString = termsEnum.term().utf8ToString();

                    PostingsEnum posting = MultiTerms.getTermPostingsEnum(indexReader, fieldinfo.name, new BytesRef(termString));

                    if (posting == null) continue; // Continue with the next term if no posting

                    int docid;
                    while ((docid = posting.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                        if (docid != docID) continue; // Filter only the docID desired

                        TermsIndexes termToList = TopTerms.getTermToList(arguments, termsEnum, indexReader, docid);

                        termsIndexes.add(termToList);

                    }

                }

                TermsIndexes.sortByRelevance(termsIndexes);
                TopTerms.showResults(top, field, arguments, termsIndexes, fieldinfo);

            }

            if (!fieldFound) System.out.println("Field not registered: " + field);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
