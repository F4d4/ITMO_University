package org.example.integration.bitrix;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnection;
import jakarta.resource.spi.ManagedConnectionFactory;
import lombok.Getter;
import lombok.Setter;

import javax.security.auth.Subject;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Set;

@Getter
@Setter
public class BitrixManagedConnectionFactory implements ManagedConnectionFactory, Serializable {

    private String webhookUrl;
    private String responsibleId;

    @Override
    public Object createConnectionFactory(ConnectionManager cxManager) throws ResourceException {
        return new BitrixConnectionFactoryImpl(this, cxManager);
    }

    @Override
    public Object createConnectionFactory() throws ResourceException {
        return new BitrixConnectionFactoryImpl(this, null);
    }

    @Override
    public ManagedConnection createManagedConnection(Subject subject, ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        return new BitrixManagedConnection(webhookUrl, responsibleId);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public ManagedConnection matchManagedConnections(Set connectionSet, Subject subject,
                                                      ConnectionRequestInfo cxRequestInfo)
            throws ResourceException {
        if (connectionSet != null && !connectionSet.isEmpty()) {
            return (ManagedConnection) connectionSet.iterator().next();
        }
        return null;
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BitrixManagedConnectionFactory other)) return false;
        return java.util.Objects.equals(webhookUrl, other.webhookUrl);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(webhookUrl);
    }
}
