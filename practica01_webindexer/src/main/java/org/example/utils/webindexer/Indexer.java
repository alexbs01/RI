package org.example.utils.webindexer;

import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.example.utils.arguments.Arguments;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;

public class Indexer {

    private final String hostname = InetAddress.getLocalHost().getHostName();
    private final String thName = Thread.currentThread().getName();
    private final Arguments arguments;

    public Indexer(Arguments arguments) throws UnknownHostException {
        this.arguments = arguments;
    }

    public void indexDoc(IndexWriter writer, Path LocNtag, Path Loc, org.jsoup.nodes.Document html) throws IOException {
        try (InputStream stream = Files.newInputStream(LocNtag)) {
            // make a new, empty document
            Document doc = new Document();

            Field pathField =
                    new StringField("path", LocNtag.toString(), Field.Store.YES);
            doc.add(pathField);

            doc.add(
                    new TextField("contents",
                            new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    )
            );

            // hosts
            doc.add(new StringField("hostname", hostname, Field.Store.YES));
            doc.add(new StringField("thread", thName, Field.Store.YES));

            // pesos
            doc.add(new StoredField("lockb", Files.size(Loc)));
            doc.add(new StoredField("notagskb", Files.size(LocNtag)));

            // times
            BasicFileAttributes attrs =
                    Files.readAttributes(Loc, BasicFileAttributes.class);
            FileTime creationTime = attrs.creationTime();
            FileTime lastAccessTime = attrs.lastAccessTime();
            FileTime lastModified = attrs.lastModifiedTime();

            doc.add(new StringField("creationTime", creationTime.toString(),
                    Field.Store.YES));
            doc.add(new StringField("lastAccessTime", lastAccessTime.toString(),
                    Field.Store.YES));
            doc.add(new StringField("lastModifiedTime", lastModified.toString(),
                    Field.Store.YES));

            Date creationTimeLucene = new Date(creationTime.toMillis());
            Date lastAccessTimeLucene = new Date(lastAccessTime.toMillis());
            Date lastModifiedLucene = new Date(lastModified.toMillis());

            doc.add(
                    new StringField("creationTimeLucene",
                            DateTools.dateToString(creationTimeLucene,
                                    DateTools.Resolution.SECOND),
                            Field.Store.YES));
            doc.add(
                    new StringField("lastAccesTimeLucene",
                            DateTools.dateToString(lastAccessTimeLucene,
                                    DateTools.Resolution.SECOND),
                            Field.Store.YES));
            doc.add(
                    new StringField("lastModifiedLucene",
                            DateTools.dateToString(lastModifiedLucene,
                                    DateTools.Resolution.SECOND),
                            Field.Store.YES));


            FieldType title = new FieldType();
            title.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
            title.setStored(true);    // default = false (same as Field.Store.NO)
            title.setTokenized(true);  // default = true (tokenize the content)
            title.setOmitNorms(false); // default = false (used when scoring)
            title.setStoreTermVectors(arguments.isTitleTermVectors());
            doc.add(
                    new Field("title", html.title(), title)
            );

            FieldType body = new FieldType();
            body.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
            body.setStored(true);    // default = false (same as Field.Store.NO)
            body.setTokenized(true);  // default = true (tokenize the content)
            body.setOmitNorms(false); // default = false (used when scoring)
            body.setStoreTermVectors(arguments.isBodyTermVectors());
            doc.add(
                    new Field("body", html.body().text(), body)
            );

            if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
                // New index, so we just add the document (no old document can be
                // there):
                System.out.println("adding " + LocNtag);
                writer.addDocument(doc);
            } else {
                // Existing index (an old copy of this document may have been indexed)
                // so we use updateDocument instead to replace the old one matching the
                // exact path, if present:
                System.out.println("updating " + LocNtag);
                writer.updateDocument(new Term("path", LocNtag.toString()), doc);
            }
        }
    }
}
