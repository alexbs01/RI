package org.example;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.example.utils.arguments.Arguments;
import org.example.utils.webindexer.Indexer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WebIndexer {
    public static void main(String[] args) {

        long start = System.currentTimeMillis();

        if (args.length < 4)
            throw new IllegalArgumentException("Usage: java WebIndexer -index INDEX_PATH -docs DOCS_PATH [-create] [-numThreads int] [-h] [-p] [-titleTermVectors] [-bodyTermVectors] [-analyzer Analyzer]");

        // Properties
        Properties ps = new Properties();
        String[] onlyDoms = null;

        try {
            ps.load(new FileInputStream("src/main/resources/config.properties"));
            onlyDoms = ps.get("onlyDoms").toString().split("onlyDoms=|\\s");
        } catch (FileNotFoundException e) {
            System.out.println("No properties file present in src/main/resources");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Rrls files
        final File urls = new File("src/test/resources/url");
        final File[] files = urls.listFiles();

        // Arguments
        Arguments arguments = new Arguments(args, WebIndexer.class.getSimpleName());

        final ExecutorService executor = Executors.newFixedThreadPool(arguments.getNumCores());

        if (files != null) {
            for (File file : files) {
                final Runnable work = new WorkerTh(file, arguments, onlyDoms);
                executor.execute(work);
            }
        }

        executor.shutdown();

        try {
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (final InterruptedException e) {
            e.printStackTrace();
            System.exit(-2);
        }

        if (arguments.isAplicationInformation()) {
            System.out.println("Creado índice " + arguments.getIndexPath() + " en " + (System.currentTimeMillis() - start) + " msecs");
        }
    }

    public static class WorkerTh implements Runnable {

        private final File file;
        private final Arguments arguments;
        private final String[] onlyDoms;

        public WorkerTh(final File file, final Arguments arguments, final String[] onlyDoms) {
            this.file = file;
            this.arguments = arguments;
            this.onlyDoms = onlyDoms;
        }

        @Override
        public void run() {

            // Get url files
            List<URL> lines = new ArrayList<>();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line = reader.readLine();

                while (line != null) {
                    lines.add(new URL(line));
                    line = reader.readLine();
                }

                reader.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

            HttpClient client = HttpClient.newHttpClient();
            Path Loc;
            Path LocNtag;

            for (URL url : lines) { // SearchFiles
                if (arguments.isThreadInformation()) {
                    System.out.println("Hilo " + Thread.currentThread().getName() + " comienzo url " + url);
                }

                try {
                    HttpRequest request = HttpRequest.newBuilder().uri(url.toURI()).build();

                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {

                        String body = response.body();
                        String name = url.toString().split("/{2}")[1];

                        boolean index = true;

                        if (onlyDoms != null) {
                            String[] sufix = name.split("\\.");
                            String sufixs = "." + sufix[sufix.length - 1];

                            index = Arrays.asList(onlyDoms).contains(sufixs);
                        }

                        if (index) {
                            // save .loc
                            Path path = Paths.get(arguments.getDocsPath() + "/" + name + ".loc");
                            Loc = path;
                            try (final BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                                    StandardOpenOption.CREATE)) {
                                writer.write(body);
                                writer.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            // save .loc.notags
                            Document html = Jsoup.parse(body);
                            path = Paths.get(arguments.getDocsPath() + "/" + name + ".loc.notags");
                            LocNtag = path;
                            try (final BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                                    StandardOpenOption.CREATE)) {
                                writer.write(html.title());
                                writer.newLine();
                                writer.write(html.body().text());
                                writer.flush();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            index(LocNtag, Loc, html);

                            if (arguments.isThreadInformation()) {
                                System.out.println("Hilo " + Thread.currentThread().getName() + " fin url " + url);
                            }
                        } else System.out.println("Not admitted " + name);

                    } else {
                        System.out.println("Error in " + url +
                                " response: " + response.statusCode());
                    }

                } catch (URISyntaxException | InterruptedException | IOException e) {
                    e.printStackTrace();
                }
            }
        }

        synchronized void index(Path LocNtag, Path Loc, org.jsoup.nodes.Document html) throws IOException {
            Indexer indexer = new Indexer(arguments);
            Directory dir = FSDirectory.open(arguments.getIndexPath());
            IndexWriterConfig iwc = new IndexWriterConfig(arguments.getAnalyzer());
            if (arguments.isCreate()) {
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            } else {
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            }

            try {
                IndexWriter writer = new IndexWriter(dir, iwc);
                indexer.indexDoc(writer, Loc, LocNtag, html);
                writer.commit();
                writer.close();
            } catch (IOException e) {
                index(LocNtag, Loc, html);
            }
        }
    }
}
