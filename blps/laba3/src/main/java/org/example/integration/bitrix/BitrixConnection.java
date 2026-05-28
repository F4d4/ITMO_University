package org.example.integration.bitrix;

public interface BitrixConnection extends AutoCloseable {
    Long createTask(String title, String description);
    void close();
}
