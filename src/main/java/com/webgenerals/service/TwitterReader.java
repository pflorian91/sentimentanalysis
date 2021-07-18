package com.webgenerals.service;

import com.webgenerals.util.LocalProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * TwitterReader
 *
 * @author Florian Popa fpopa1991@gmail.com
 */
public class TwitterReader {

    private final Twitter twitter;
    private final CSVService csvService = new CSVService();
    private Logger logger = LoggerFactory.getLogger(TwitterReader.class);

    public TwitterReader() {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        LocalProperties properties = new LocalProperties();

        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(properties.getConsumerKey())
                .setOAuthConsumerSecret(properties.getConsumerSecret())
                .setOAuthAccessToken(properties.getAccessToken())
                .setOAuthAccessTokenSecret(properties.getAccessSecret());

        TwitterFactory tf = new TwitterFactory(cb.build());
        twitter = tf.getInstance();
    }

    public void extractTweets(String query) {
        if (csvService.isExistingRawDownloadForQuery(query)) {
            logger.info("Data already exists for \"{}\"", query);
            return;
        }

        csvService.setupRawFileAndWriter(query);
        String[] headerRecord = {"#", "Tweet", "CreatedAt", "ID"};
        csvService.writeToCsv(headerRecord);

        try {
            extractTweetsAndWriteInCSV(query);
        } catch (TwitterException e) {
            logger.error("Error encountered while extracting Twitter data", e);
        }

        csvService.closeWriter();
    }

    private void extractTweetsAndWriteInCSV(String queryText) throws TwitterException {
        Query query = new Query(queryText);
        query.setLang("en");

        QueryResult result;
        AtomicInteger tweetsReceived = new AtomicInteger();

        do {
            result = twitter.search(query);

            result.getTweets()
                    .stream()
                    .filter(status -> !status.isRetweet() && status.getRetweetCount() < 1)
                    .forEach(status -> {
                        tweetsReceived.getAndIncrement();

                        csvService.writeToCsv(
                                new String[]{
                                        String.valueOf(tweetsReceived.get()),
                                        status.getText(),
                                        status.getCreatedAt().toString(),
                                        String.valueOf(status.getId())
                                }
                        );
                    });

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                logger.error("Exception while trying to sleep thread", e);
            }
        } while ((query = result.nextQuery()) != null);
    }
}
