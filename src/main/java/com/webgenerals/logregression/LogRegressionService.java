package com.webgenerals.logregression;

import com.aliasi.classify.ConditionalClassification;
import com.aliasi.classify.LMClassifier;
import com.aliasi.util.AbstractExternalizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static com.webgenerals.logregression.LogRegressionTrainingService.OUTPUT_DATA_DIRECTORY;
import static com.webgenerals.logregression.LogRegressionTrainingService.OUTPUT_MODEL_FILENAME;
import static com.webgenerals.service.DataProcessingService.INSIGNIFICANT_VALUE;

/**
 * LogRegressionService
 *
 * @author Florian Popa fpopa1991@gmail.com
 */
public class LogRegressionService {

    private static final String POSITIVE_CATEGORY = "Positive";
    private static final String NEGATIVE_CATEGORY = "Negative";
    private Logger logger = LoggerFactory.getLogger(LogRegressionTrainingService.class);

    private LMClassifier lmClassifier;

    public LogRegressionService() {
        String absolutePath = System.getProperty("user.dir");

        try {
            //read the model classifier
            lmClassifier = (LMClassifier) AbstractExternalizable.readObject(new File(absolutePath + OUTPUT_DATA_DIRECTORY + File.separator + OUTPUT_MODEL_FILENAME));
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            logger.error("Could not initialize logistic regression model classifier", e);
        }

        for (String sentimentCategory : lmClassifier.categories()) {
            logger.info("Sentiment category {}", sentimentCategory);
        }
    }

    public int calculateSentiment(String text) {
        ConditionalClassification classification = lmClassifier.classify(text);

        if (classification.bestCategory().equals(POSITIVE_CATEGORY)) {
            return 1;
        }

        if (classification.bestCategory().equals(NEGATIVE_CATEGORY)) {
            return 0;
        }

        return INSIGNIFICANT_VALUE;
    }
}
