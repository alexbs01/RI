package org.example.utils.queries;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.*;
import java.util.*;

public class QueryC {
    public static List<Query> parseQueries(String opt) throws FileNotFoundException {
        List<Query> queries;

        var inputStream = new FileInputStream("trec-covid/queries.jsonl");
        ObjectReader reader = JsonMapper.builder().findAndAddModules().build().readerFor(Query.class);
        try {
            MappingIterator<Query> iterator = reader.readValues(inputStream);

            queries = iterator.readAll();

            if (!Objects.equals(opt, "all")) {
                if (opt.contains("-")) {
                    String[] opts = opt.split("-");
                    queries.removeIf(x -> Integer.parseInt(x.id()) < Integer.parseInt(opts[0]) || Integer.parseInt(x.id()) > Integer.parseInt(opts[1]));
                } else {
                    queries.removeIf(x -> !Objects.equals(x.id(), opt));
                }
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return queries;
    }

    public static List<Map<String, Integer>> getRelevance() throws IOException {
        Map<String, Integer> idDocRelv = new HashMap<>();
        Map<String, Integer> numRelv = new HashMap<>();

        try (BufferedReader readerRev = new BufferedReader(new FileReader("trec-covid/qrels/test.tsv"))) {
            readerRev.readLine();
            String line = readerRev.readLine();
            while (line != null) {
                String[] vals = line.split("\t");
                int relv = Integer.parseInt(vals[2]);
                idDocRelv.put(vals[0] + vals[1], relv);
                if (relv > 0) numRelv.put(vals[0], numRelv.get(vals[0]) == null ? 1 : numRelv.get(vals[0]) + 1);
                line = readerRev.readLine();
            }
        }

        List<Map<String, Integer>> out = new ArrayList<>();
        out.add(idDocRelv);
        out.add(numRelv);

        return out;
    }
}
