package org.example.integration.bitrix;

public class BitrixConnectionImpl implements BitrixConnection {

    private BitrixManagedConnection managedConnection;
    private boolean valid = true;

    public BitrixConnectionImpl(BitrixManagedConnection managedConnection) {
        this.managedConnection = managedConnection;
    }

    @Override
    public Long createTask(String title, String description) {
        checkValid();
        return managedConnection.createTask(title, description);
    }

    @Override
    public void close() {
        if (valid) {
            valid = false;
            managedConnection.notifyConnectionClosed();
        }
    }

    void invalidate() {
        valid = false;
    }

    void setManagedConnection(BitrixManagedConnection mc) {
        this.managedConnection = mc;
    }

    private void checkValid() {
        if (!valid) {
            throw new IllegalStateException("Соединение с Bitrix24 уже закрыто");
        }
    }
}
