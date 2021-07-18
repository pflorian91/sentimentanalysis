package com.webgenerals.service;

/**
 * TweetProcessor
 *
 * @author Florian Popa fpopa1991@gmail.com
 */
public interface TweetProcessor {
    void process(int counter, String tweetText, String tweetID);
}
