package com.kangaroohy.milo.model;

import lombok.Value;
import org.eclipse.milo.opcua.stack.core.types.builtin.LocalizedText;
import org.eclipse.milo.opcua.stack.core.types.builtin.QualifiedName;
import org.eclipse.milo.opcua.stack.core.types.enumerated.NodeClass;

/** A browsed node whose nodeId can be passed directly to read/write/subscribe APIs. */
@Value
public class BrowseNode {
    String nodeId;
    QualifiedName browseName;
    LocalizedText displayName;
    NodeClass nodeClass;
}
