package org.example.integration.bitrix;

import jakarta.resource.ResourceException;

public class BitrixConnectionFactoryImpl implements BitrixConnectionFactory {

    private final BitrixManagedConnectionFactory managedConnectionFactory;

    public BitrixConnectionFactoryImpl(BitrixManagedConnectionFactory mcf) {
        this.managedConnectionFactory = mcf;
    }

    @Override
    public BitrixConnection getConnection() {
        try {
            BitrixManagedConnection mc = (BitrixManagedConnection)
                    managedConnectionFactory.createManagedConnection(null, null);
            return (BitrixConnection) mc.getConnection(null, null);
        } catch (ResourceException e) {
            throw new RuntimeException("Не удалось получить соединение с Bitrix24", e);
        }
    }
}
