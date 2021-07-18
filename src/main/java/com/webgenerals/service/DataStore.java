package com.webgenerals.service;

/**
 * DataStore
 *
 * @author Florian Popa fpopa1991@gmail.com
 */
class DataStore {

    private static int nbPositive = 0;
    private static int nbNegative = 0;
    private static int lrPositive = 0;
    private static int lrNegative = 0;
    private static int tweetsCount = 0;
    private static String query = "";
    private static DataStore instance;

    private DataStore() {
    }

    static DataStore getInstance() {
        if (instance == null) {
            instance = new DataStore();
        }

        return instance;
    }

    static void increaseLRPositive() {
        DataStore.lrPositive++;
    }

    static void increaseLRNegative() {
        DataStore.lrNegative++;
    }

    static int getLrPositive() {
        return lrPositive;
    }

    static int getLrNegative() {
        return lrNegative;
    }

    static void increaseNBPositive() {
        DataStore.nbPositive++;
    }

    static void increaseNBNegative() {
        DataStore.nbNegative++;
    }

    static int getNBPositive() {
        return nbPositive;
    }

    static int getNBNegative() {
        return nbNegative;
    }

    static String getQuery() {
        return query;
    }

    static void setQuery(String query) {
        DataStore.query = query;
    }

    static int getTweetsCount() {
        return tweetsCount;
    }

    static void setTweetsCount(int tweetsCount) {
        DataStore.tweetsCount = tweetsCount;
    }

    @Override
    public String toString() {
        return "Data store: " + super.hashCode() +
                "\nNB Positive: " + nbPositive +
                "\nNB Negative: " + nbNegative +
                "\nLR Positive: " + lrPositive +
                "\nLR Negative: " + lrNegative +
                "\nTweets count: " + tweetsCount +
                "\nQuery: " + query;
    }
}
