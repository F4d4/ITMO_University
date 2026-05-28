package org.example.integration.bitrix;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;

public class BitrixConnectionFactoryImpl implements BitrixConnectionFactory {

    private final BitrixManagedConnectionFactory managedConnectionFactory;
    private final ConnectionManager connectionManager;

    public BitrixConnectionFactoryImpl(BitrixManagedConnectionFactory mcf, ConnectionManager cm) {
        this.managedConnectionFactory = mcf;
        this.connectionManager = cm;
    }

    @Override
    public BitrixConnection getConnection() {
        try {
            if (connectionManager != null) {
                return (BitrixConnection) connectionManager.allocateConnection(managedConnectionFactory, null);
            }
            BitrixManagedConnection mc = (BitrixManagedConnection)
                    managedConnectionFactory.createManagedConnection(null, null);
            return (BitrixConnection) mc.getConnection(null, null);
        } catch (ResourceException e) {
            throw new RuntimeException("Не удалось получить соединение с Bitrix24", e);
        }
    }
}
