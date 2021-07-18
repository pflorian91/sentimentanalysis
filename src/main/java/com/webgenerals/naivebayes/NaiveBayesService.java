package com.webgenerals.naivebayes;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.apache.mahout.classifier.naivebayes.NaiveBayesModel;
import org.apache.mahout.classifier.naivebayes.StandardNaiveBayesClassifier;
import org.apache.mahout.classifier.naivebayes.training.TrainNaiveBayesJob;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterable;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.vectorizer.SparseVectorsFromSequenceFiles;
import org.apache.mahout.vectorizer.TFIDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * NaiveBayesService
 *
 * @author Florian Popa fpopa1991@gmail.com
 */
public class NaiveBayesService {

    private static String TRAINING_DATA_DIRECTORY = "/training-data";
    private static String SEQUENCE_DATA_FILE_PATH = "naive-bayes/tweets-seq";
    private static String LABEL_INDEX_PATH = "naive-bayes/labelindex";
    private static String MODEL_PATH = "naive-bayes/model";
    private static String VECTORS_PATH = "naive-bayes/tweets-vectors";
    private static String DICTIONARY_PATH = "naive-bayes/tweets-vectors/dictionary.file-0";
    private static String DOCUMENT_FREQUENCY_PATH = "naive-bayes/tweets-vectors/df-count/part-r-00000";

    private Configuration configuration = new Configuration();
    private Logger logger = LoggerFactory.getLogger(NaiveBayesService.class);

    public void trainNaiveBayesModel(String trainingDataFileName) {
        if (isExistingDictionary()) {
            logger.info("Training data already present for Naive Bayes");
            return;
        }

        try {
            extractTrainingDataToSequenceFile(trainingDataFileName);
        } catch (Exception e) {
            logger.error("Exception when extracting training data to sequence file", e);
        }

        try {
            sequenceFileToSparseVector();
        } catch (Exception e) {
            logger.error("Exception when creating sparse vectors", e);
        }

        try {
            trainNaiveBayesModel();
        } catch (Exception e) {
            logger.error("Exception when training naive bayes model", e);
        }

    }

    private boolean isExistingDictionary() {
        File file = new File(DICTIONARY_PATH);
        return file.exists();
    }

    private void extractTrainingDataToSequenceFile(String trainingDataFileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream(TRAINING_DATA_DIRECTORY + File.separator + trainingDataFileName)
        ));

        FileSystem fs = FileSystem.getLocal(configuration);
        Path seqFilePath = new Path(SEQUENCE_DATA_FILE_PATH);
        fs.delete(seqFilePath, false);
        SequenceFile.Writer writer = SequenceFile.createWriter(fs, configuration, seqFilePath, Text.class, Text.class);

        int count = 0;
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split("\t");
                writer.append(new Text("/" + tokens[0] + "/tweet" + count++),
                        new Text(tokens[1]));
            }
        } finally {
            reader.close();
            writer.close();
        }
    }

    private void sequenceFileToSparseVector() throws Exception {
        SparseVectorsFromSequenceFiles svfsf = new SparseVectorsFromSequenceFiles();
        svfsf.run(new String[]{"-i", SEQUENCE_DATA_FILE_PATH, "-o", VECTORS_PATH,
                "-ow"});
    }

    private void trainNaiveBayesModel() throws Exception {
        TrainNaiveBayesJob trainNaiveBayes = new TrainNaiveBayesJob();
        trainNaiveBayes.setConf(configuration);
        trainNaiveBayes.run(new String[]{"-i",
                VECTORS_PATH + "/tfidf-vectors", "-o", MODEL_PATH, "-li",
                LABEL_INDEX_PATH, "-el", "-c", "-ow"});
    }

    // classification
    public int classifyNewTweet(String tweet) throws IOException {
        Map<String, Integer> dictionary = readDictionary(configuration,
                new Path(DICTIONARY_PATH));
        Map<Integer, Long> documentFrequency = readDocumentFrequency(
                configuration, new Path(DOCUMENT_FREQUENCY_PATH));

        Multiset<String> words = ConcurrentHashMultiset.create();

        // Extract the words from the new tweet using Lucene
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
        TokenStream tokenStream = analyzer.tokenStream("text",
                new StringReader(tweet));
        CharTermAttribute termAttribute = tokenStream
                .addAttribute(CharTermAttribute.class);
        tokenStream.reset();
        int wordCount = 0;
        while (tokenStream.incrementToken()) {
            if (termAttribute.length() > 0) {
                String word = tokenStream.getAttribute(CharTermAttribute.class)
                        .toString();
                Integer wordId = dictionary.get(word);
                // If the word is not in the dictionary, skip it
                if (wordId != null) {
                    words.add(word);
                    wordCount++;
                }
            }
        }
        tokenStream.end();
        tokenStream.close();

        int documentCount = documentFrequency.get(-1).intValue();

        // Create a vector for the new tweet (wordId => TFIDF weight)
        Vector vector = new RandomAccessSparseVector(10000);
        TFIDF tfidf = new TFIDF();
        for (Multiset.Entry<String> entry : words.entrySet()) {
            String word = entry.getElement();
            int count = entry.getCount();
            Integer wordId = dictionary.get(word);
            Long freq = documentFrequency.get(wordId);
            double tfIdfValue = tfidf.calculate(count, freq.intValue(),
                    wordCount, documentCount);
            vector.setQuick(wordId, tfIdfValue);
        }

        // Model is a matrix (wordId, labelId) => probability score
        NaiveBayesModel model = NaiveBayesModel.materialize(
                new Path(MODEL_PATH), configuration);
        StandardNaiveBayesClassifier classifier = new StandardNaiveBayesClassifier(
                model);

        // With the classifier, we get one score for each label.The label with
        // the highest score is the one the tweet is more likely to be
        // associated to
        Vector resultVector = classifier.classifyFull(vector);
        double bestScore = -Double.MAX_VALUE;
        int bestCategoryId = -1;
        for (Element element : resultVector.all()) {
            int categoryId = element.index();
            double score = element.get();
            if (score > bestScore) {
                bestScore = score;
                bestCategoryId = categoryId;
            }
        }
        analyzer.close();

        return bestCategoryId;
    }

    private Map<String, Integer> readDictionary(Configuration conf, Path dictionaryPath) {
        Map<String, Integer> dictionnary = new HashMap<String, Integer>();
        for (Pair<Text, IntWritable> pair : new SequenceFileIterable<Text, IntWritable>(
                dictionaryPath, true, conf)) {
            dictionnary.put(pair.getFirst().toString(), pair.getSecond().get());
        }
        return dictionnary;
    }

    private Map<Integer, Long> readDocumentFrequency(Configuration conf, Path documentFrequencyPath) {
        Map<Integer, Long> documentFrequency = new HashMap<Integer, Long>();
        for (Pair<IntWritable, LongWritable> pair : new SequenceFileIterable<IntWritable, LongWritable>(
                documentFrequencyPath, true, conf)) {
            documentFrequency.put(pair.getFirst().get(), pair.getSecond().get());
        }
        return documentFrequency;
    }
}
