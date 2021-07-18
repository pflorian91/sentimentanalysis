package com.webgenerals.service;

import com.webgenerals.logregression.LogRegressionService;
import com.webgenerals.logregression.LogRegressionTrainingService;
import com.webgenerals.naivebayes.NaiveBayesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * DataProcessingService
 *
 * @author Florian Popa fpopa1991@gmail.com
 */
public class DataProcessingService {

    // TODO receive from properties?
    private static final String TRAINING_DATA_FILE_NAME = "training-data-1578627.txt";
    public static final int INSIGNIFICANT_VALUE = -99;
    private Logger logger = LoggerFactory.getLogger(DataProcessingService.class);

    private DataStore dataStore = DataStore.getInstance();

    private TwitterReader twitterReader = new TwitterReader();
    private CSVService csvService = new CSVService();
    private NaiveBayesService naiveBayesService = new NaiveBayesService();
    private LogRegressionTrainingService logRegressionTrainingService = new LogRegressionTrainingService();
    private LogRegressionService logRegressionService = new LogRegressionService();

    public void extractAndProcessData(String query) {
        // read data from twitter
        twitterReader.extractTweets(query);
        DataStore.setQuery(query);

        // train NB
        naiveBayesService.trainNaiveBayesModel(TRAINING_DATA_FILE_NAME);

        // train log regression
        logRegressionTrainingService.train();


        // setup processing file
        csvService.setupProcessedFileAndWriter(query);
        String[] headerRecord = {"#", "NB Sentiment", "LR Sentiment", "Tweet", "Raw Tweet", "Tweet ID"};
        csvService.writeToCsv(headerRecord);

        // processing
        csvService.readRawCSV(query, processLine());

        csvService.closeWriter();


        // analysis results
        csvService.setupAnalysisFileAndWriter(query);
        csvService.writeToCsv(new String[]{"Tweets count", "Query", "-", "-", "-"});
        csvService.writeToCsv(new String[]{String.valueOf(DataStore.getTweetsCount()), query, "-"});

        String[] analysisHead = {"Method name", "Positive", "Positive %", "Negative", "Negative %"};
        csvService.writeToCsv(analysisHead);
        double positivePerc = 100.0 * DataStore.getNBPositive() / DataStore.getTweetsCount();
        double negativePerc = 100.0 * DataStore.getNBNegative() / DataStore.getTweetsCount();

        csvService.writeToCsv(new String[]{
                "Naive Bayes",
                String.valueOf(DataStore.getNBPositive()),
                String.valueOf(positivePerc),
                String.valueOf(DataStore.getNBNegative()),
                String.valueOf(negativePerc)
        });

        double lrPositivePerc = 100.0 * DataStore.getLrPositive() / DataStore.getTweetsCount();
        double lrNegativePerc = 100.0 * DataStore.getLrNegative() / DataStore.getTweetsCount();

        csvService.writeToCsv(new String[]{
                "Logistic regression",
                String.valueOf(DataStore.getLrPositive()),
                String.valueOf(lrPositivePerc),
                String.valueOf(DataStore.getLrNegative()),
                String.valueOf(lrNegativePerc)
        });

        csvService.closeWriter();

        logger.info("Analysis result {}", dataStore);
    }

    private TweetProcessor processLine() {
        return (counter, tweetText, tweetID) -> {
            String processedText = tweetText.trim()
                    // remove links
                    .replaceAll("http.*?[\\S]+", "")
                    // remove username
                    .replaceAll("@[\\S]+", "")
                    // extract words from hash tags
                    .replaceAll("#", "")
                    // correct all multiple white spaces to a single white space
                    .replaceAll("[\\s]+", " ");

            int nbSentiment = getNBSentiment(processedText);
            int lrSentiment = getLRSentiment(processedText);

            csvService.writeToCsv(
                    new String[]{
                            String.valueOf(counter),
                            String.valueOf(nbSentiment),
                            String.valueOf(lrSentiment),
                            processedText,
                            tweetText,
                            tweetID
                    }
            );
        };
    }

    private int getNBSentiment(String text) {
        int nbSentiment = INSIGNIFICANT_VALUE;
        try {
            nbSentiment = naiveBayesService.classifyNewTweet(text);

            if (nbSentiment == 1) {
                DataStore.increaseNBPositive();
            } else {
                DataStore.increaseNBNegative();
            }
        } catch (IOException e) {
            logger.error("Failed to classify tweet {}", text, e);
        }

        return nbSentiment;
    }

    private int getLRSentiment(String text) {
        int lrSentiment = logRegressionService.calculateSentiment(text);

        if (lrSentiment == 1) {
            DataStore.increaseLRPositive();
        }

        if (lrSentiment == 0) {
            DataStore.increaseLRNegative();
        }

        return lrSentiment;
    }

}
