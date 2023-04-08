package dev.polluxus.scryfall_projects.cmd;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.text.SimpleDateFormat;
import java.time.Instant;

public class Configuration {

    private final ObjectMapper mapper;

    private final String inputPath;

    private final String outputPath;

    private final Integer parallelism;

    public Configuration(String inputPath, String outputPath, String parallelism) {
        this.inputPath = inputPath;
        this.outputPath = outputPath;
        this.parallelism = Integer.parseInt(parallelism);
        this.mapper = initMapper();
    }

    private ObjectMapper initMapper() {

        return new ObjectMapper()
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule())
                .setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public ObjectMapper mapper() {
        return mapper;
    }

    public String inputPath() {
        if (inputPath == null) {
            return "src/main/resources/card_data/default-cards-20230403090602.json";
        }
        return inputPath;
    }

    public String outputPath() {
        if (outputPath == null) {
            return "output/output-" + Instant.now().getEpochSecond() + ".sql";
        }
        return outputPath;
    }

    final static int MAX_PARALLELISM = 50;

    public Integer parallelism() {
        if (parallelism == null) {
            return 1;
        }
        return Math.min(parallelism, MAX_PARALLELISM);
    }
}
