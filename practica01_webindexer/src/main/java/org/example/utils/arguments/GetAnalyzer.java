package org.example.utils.arguments;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.gl.GalicianAnalyzer;
import org.apache.lucene.analysis.it.ItalianAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;


public class GetAnalyzer {
    String analyzer = "";

    public GetAnalyzer(String analyzer) {
        this.analyzer = analyzer;
    }

    public GetAnalyzer() {
    }

    public Analyzer get() {
        Analyzer analyzer;

        switch(this.analyzer) {
            case "spanish": analyzer = new SpanishAnalyzer(); break;
            case "english": analyzer = new EnglishAnalyzer(); break;
            case "galician": analyzer = new GalicianAnalyzer(); break;
            case "italian": analyzer = new ItalianAnalyzer(); break;
            case "french": analyzer = new FrenchAnalyzer(); break;
            case "german": analyzer = new GermanAnalyzer(); break;
            default: analyzer = new StandardAnalyzer();
        };

        return analyzer;
    }
}
