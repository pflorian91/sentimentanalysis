package com.webgenerals;

import com.webgenerals.service.DataProcessingService;

/**
 * @author Florian Popa fpopa1991@gmail.com
 */
public class TwitterSentimentAnalysisApp {

    public static void main(String[] args) {

//        String query = "(covid19 vaccine) OR (covid vaccine) OR (covid19 vaccinated) OR (covid vaccinated) OR (sars-cov2 vaccine) OR (sars-cov2 vaccinated)";

//        String query = "(car OR vehicle) AND electric";

//        String query = "nike (workout OR training OR running OR run OR gear OR cycling)";
        String query = "reebok (workout OR training OR running OR run OR gear OR cycling)";
//        String query = "(underarmour or ua) and (workout OR training OR running OR run OR gear OR cycling)";
//        String query = "adidas and (workout OR training OR running OR run OR gear OR cycling)";

        DataProcessingService dataProcessingService = new DataProcessingService();
        dataProcessingService.extractAndProcessData(query);

    }
}
