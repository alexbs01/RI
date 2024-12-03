package org.example;

import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import org.example.utils.arguments.Arguments;
import org.example.utils.topterms.TermsIndexes;
import org.example.utils.topterms.TopTerms;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class TopTermsInField {
    public static void main(String[] args) {
        // arguments
        if (args.length < 4)
            throw new IllegalArgumentException("Usage: java TopTermsInField -index <path> -field <field> [-top <n>] [-outfile <path>]");

        Arguments arguments = new Arguments(args, TopTermsInField.class.getSimpleName());

        Path index = arguments.getIndexPath();
        String field = arguments.getField();
        int top = arguments.getTopN();

        // open index
        DirectoryReader indexReader = TopTerms.getIndexReader(index);

        // Field info
        final FieldInfos fieldinfos = FieldInfos.getMergedFieldInfos(indexReader);

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
                Map<String, TermsIndexes> mapTerms = new TreeMap<>();

                while (termsEnum.next() != null) {
                    String termString = termsEnum.term().utf8ToString();

                    PostingsEnum posting = MultiTerms.getTermPostingsEnum(indexReader, fieldinfo.name, new BytesRef(termString));

                    if (posting == null) continue; // Continue with the next term if no posting

                    TermsIndexes termToList = TopTerms.getTermToList(arguments, termsEnum, indexReader, 0);
                    termsIndexes.add(termToList);
                }
                System.out.println(termsIndexes.get(0).getDocFreq());

                TermsIndexes.sortByDF(termsIndexes);
                TopTerms.showResults(top, field, arguments, termsIndexes, fieldinfo);

            }

            if (!fieldFound) System.out.println("Field not registered: " + field);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
