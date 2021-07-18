package com.webgenerals.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CSVService
 *
 * @author Florian Popa fpopa1991@gmail.com
 */
class CSVService {

    private static final String TEST_DATA_DIRECTORY = "test-data";
    private static final String RAW_DATA_DIRECTORY = "raw";
    private static final String PROCESSED_DATA_DIRECTORY = "processed";
    private static final String ANALYSIS_DATA_DIRECTORY = "analysis";
    private CSVWriter writer;
    private Logger logger = LoggerFactory.getLogger(CSVService.class);

    // reading test-data
    void readRawCSV(String query, TweetProcessor tweetProcessor) {
        String fileName = extractFileName(query);
        String filePath = getRawFilePath(fileName);

        try (
                Reader reader = Files.newBufferedReader(Paths.get(filePath));
                CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(1).build();
        ) {
            String[] nextRecord;
            AtomicInteger counter = new AtomicInteger();

            while ((nextRecord = csvReader.readNext()) != null) {
                tweetProcessor.process(counter.getAndIncrement(), nextRecord[1], nextRecord[3]);
            }

            DataStore.setTweetsCount(counter.get());

            logger.info("Processed {} tweets", counter.get());
        } catch (IOException e) {
            logger.error("Could not read the raw CSV file", e);
        } catch (CsvValidationException e) {
            logger.error("An exception occurred when reading next line", e);
        }
    }

    // writing analysis result data
    void setupAnalysisFileAndWriter(String query) {
        String fileName = extractFileName(query);

        File file = new File(getAnalysisFilePath(fileName));
        file.getParentFile().mkdirs();
        try {
            file.createNewFile();
            writer = new CSVWriter(new FileWriter(file));
        } catch (IOException e) {
            logger.error("Failed to create file", e);
        }
    }

    // writing processed data
    void setupProcessedFileAndWriter(String query) {
        String fileName = extractFileName(query);

        File file = new File(getProcessedFilePath(fileName));
        file.getParentFile().mkdirs();
        try {
            file.createNewFile();
            writer = new CSVWriter(new FileWriter(file));
        } catch (IOException e) {
            logger.error("Failed to create file", e);
        }
    }


    // WRITING test-data
    boolean isExistingRawDownloadForQuery(String query) {
        String fileName = extractFileName(query);
        File file = new File(getRawFilePath(fileName));
        return file.exists();
    }

    void setupRawFileAndWriter(String query) {
        String fileName = extractFileName(query);

        File file = new File(getRawFilePath(fileName));
        file.getParentFile().mkdirs();
        try {
            file.createNewFile();
            writer = new CSVWriter(new FileWriter(file));
        } catch (IOException e) {
            logger.error("Failed to create file", e);
        }
    }

    void writeToCsv(String[] data) {
        writer.writeNext(data);
    }

    void closeWriter() {
        try {
            writer.close();
        } catch (IOException e) {
            logger.error("Failed to close writer", e);
        }
    }

    private String getRawFilePath(String fileName) {
        return TEST_DATA_DIRECTORY + File.separator + RAW_DATA_DIRECTORY + File.separator + fileName;
    }

    private String getProcessedFilePath(String fileName) {
        return TEST_DATA_DIRECTORY + File.separator + PROCESSED_DATA_DIRECTORY + File.separator + fileName;
    }

    private String getAnalysisFilePath(String fileName) {
        return TEST_DATA_DIRECTORY + File.separator + ANALYSIS_DATA_DIRECTORY + File.separator + fileName;
    }

    private String extractFileName(String query) {
        query = query.length() > 30 ? query.substring(0, 30) : query;
        return query.toLowerCase().replaceAll("\\s", "_").replaceAll("\\W+", "") + ".csv";
    }
}
