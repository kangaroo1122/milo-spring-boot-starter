package com.kangaroohy.milo.exception;

import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;

import java.util.List;

/** A partial batch failure. Timeout/communication failures do not prove a write was not applied. */
public class BatchWriteException extends IllegalStateException {
    private final List<String> identifiers;
    private final List<StatusCode> statuses;

    public BatchWriteException(List<String> identifiers, List<StatusCode> statuses, String message) {
        super(message);
        this.identifiers = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(identifiers));
        this.statuses = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(statuses));
    }

    public List<String> getIdentifiers() {
        return identifiers;
    }

    public List<StatusCode> getStatuses() {
        return statuses;
    }
}
