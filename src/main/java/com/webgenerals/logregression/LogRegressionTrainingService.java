package com.webgenerals.logregression;

import com.aliasi.classify.Classification;
import com.aliasi.classify.Classified;
import com.aliasi.classify.DynamicLMClassifier;
import com.aliasi.classify.LMClassifier;
import com.aliasi.corpus.ObjectHandler;
import com.aliasi.util.AbstractExternalizable;
import com.aliasi.util.Compilable;
import com.aliasi.util.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * LogRegressionTrainingService
 *
 * @author Florian Popa fpopa1991@gmail.com
 */
public class LogRegressionTrainingService {

    private static final String TRAINING_DATA_DIRECTORY = "/src/main/resources/training-data/logistic-regression";
    static final String OUTPUT_DATA_DIRECTORY = "/logistic-regression";
    static final String OUTPUT_MODEL_FILENAME = "classifier.txt";

    private Logger logger = LoggerFactory.getLogger(LogRegressionTrainingService.class);

    public void train() {
        String absolutePath = System.getProperty("user.dir");
        File outputFile = new File(absolutePath + OUTPUT_DATA_DIRECTORY + File.separator + OUTPUT_MODEL_FILENAME);
        if (outputFile.exists()) {
            logger.info("Training data already present for Logistic Regression");
            return;
        }

        logger.info("Started training process for logistic regression");

        File trainingDirectory = new File(absolutePath + TRAINING_DATA_DIRECTORY);
        String[] trainingCategories = trainingDirectory.list();

        //nGram level (combination of words to consider for classification) any value between 7 and 12
        int nGramLevel = 8;

        //categories of the text i.e negative and positive text
        String[] modelCategories = {"Positive", "Negative"};

        // Language model classifier
        LMClassifier languageModelClassifier = DynamicLMClassifier.createNGramProcess(modelCategories, nGramLevel);

        // read all the files and train the model
        for (String trainingCategory : trainingCategories) {
            Classification classification = new Classification(trainingCategory);
            File file = new File(trainingDirectory, trainingCategory);
            File[] trainingFiles = file.listFiles();

            //read all the files until null
            if (trainingFiles != null) {
                for (File trainingFile : trainingFiles) {
                    String tweet = null;

                    try {
                        tweet = Files.readFromFile(trainingFile, "ISO-8859-1");
                    } catch (IOException e) {
                        logger.error("Failed to read training file", e);
                    }

                    Classified<String> classified = new Classified<String>(tweet, classification);
                    ((ObjectHandler<Classified<String>>) languageModelClassifier).handle(classified);
                }
            }

            try {
                // model is created here, writing serialize object to classifier.txt file

                outputFile.getParentFile().mkdirs();
                AbstractExternalizable.compileTo((Compilable) languageModelClassifier, outputFile);

                logger.info("Successfully created a model for category {}", trainingCategory);
            } catch (IOException e) {
                logger.error("Failed to output model file", e);
            }
        }
    }

}
