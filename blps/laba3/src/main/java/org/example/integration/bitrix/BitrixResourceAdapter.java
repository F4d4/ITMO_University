package org.example.integration.bitrix;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ActivationSpec;
import jakarta.resource.spi.BootstrapContext;
import jakarta.resource.spi.ResourceAdapterInternalException;
import jakarta.resource.spi.endpoint.MessageEndpointFactory;
import jakarta.transaction.xa.XAResource;

import java.io.Serializable;

public class BitrixResourceAdapter implements jakarta.resource.spi.ResourceAdapter, Serializable {

    @Override
    public void start(BootstrapContext ctx) throws ResourceAdapterInternalException {
    }

    @Override
    public void stop() {
    }

    @Override
    public void endpointActivation(MessageEndpointFactory endpointFactory, ActivationSpec spec)
            throws ResourceException {
    }

    @Override
    public void endpointDeactivation(MessageEndpointFactory endpointFactory, ActivationSpec spec) {
    }

    @Override
    public XAResource[] getXAResources(ActivationSpec[] specs) throws ResourceException {
        return new XAResource[0];
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof BitrixResourceAdapter;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
