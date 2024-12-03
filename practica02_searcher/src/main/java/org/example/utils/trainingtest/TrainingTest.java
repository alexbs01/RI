package org.example.utils.trainingtest;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;

public class TrainingTest {
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
}
