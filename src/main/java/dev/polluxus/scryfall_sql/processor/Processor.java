package dev.polluxus.scryfall_sql.processor;

import java.io.Writer;
import java.util.List;

public interface Processor<T> {

    void process(List<T> elements);

    void start();

    void commit();
}