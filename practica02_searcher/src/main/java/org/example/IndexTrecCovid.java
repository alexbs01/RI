package org.example;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;
import org.example.utils.arguments.Arguments;
import org.example.utils.Covid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IndexTrecCovid {
    public static void main(String[] args) {

        // Arguments
        if (args.length != 9) {
            throw new IllegalArgumentException("Usage: IndexTrecCovid -openmode [APPEND | CREATE | CREATE_OR_APPEND]" +
                    " -index <path> -docs <path> -indexingmodel [jm <lambda> | bm25 <k1>]");
        }
        Arguments arguments = new Arguments(args, IndexTrecCovid.class.getSimpleName());

        IndexWriterConfig.OpenMode openMode = arguments.getOpenMode();
        Path indexPath = arguments.getIndexPath();
        Path docsPath = arguments.getDocsPath();
        Similarity indexingModel = arguments.getSearch();

        try {
            // File parsing
            List<Covid> covids = new ArrayList<>();
            for (File file : Objects.requireNonNull(docsPath.toFile().listFiles())) {
                covids.addAll(parsDoc(file));
            }

            // Set writer
            IndexWriterConfig iwc = new IndexWriterConfig(new StandardAnalyzer());
            iwc.setSimilarity(indexingModel);
            iwc.setOpenMode(openMode);

            // Write docs
            try (IndexWriter writer = new IndexWriter(FSDirectory.open(indexPath), iwc)) {
                indexList(covids, writer);
                writer.commit();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void indexList(List<Covid> covids, IndexWriter writer) {
        for (Covid covid : covids) {
            Document doc = new Document();
            Field id = new StringField("id", covid.id(), Field.Store.YES);
            Field title = new TextField("title", covid.title(), Field.Store.YES);
            Field text = new TextField("text", covid.text(), Field.Store.YES);
            Field url = new StringField("url", covid.url(), Field.Store.YES);
            Field pubmed = new StringField("pubmed_id", covid.pubmed_id(), Field.Store.YES);

            doc.add(id);
            doc.add(title);
            doc.add(text);
            doc.add(url);
            doc.add(pubmed);

            try {
                writer.addDocument(doc);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static List<Covid> parsDoc(File file) throws FileNotFoundException {
        List<Covid> covids = new ArrayList<>();

        if (file.getName().endsWith(".jsonl")) {
            var inputStream = new FileInputStream(file);
            ObjectReader reader = JsonMapper.builder().findAndAddModules().build().readerFor(Covid.class);
            try {
                MappingIterator<Covid> iterator = reader.readValues(inputStream);
                covids = iterator.readAll();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("File " + file.getName() + " parsed.");

        return covids;
    }
}
