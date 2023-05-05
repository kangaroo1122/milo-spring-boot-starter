package com.kangaroohy.milo.runner;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.Identifiers;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.unsigned.UShort;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @since 2020/4/14
 */
@Slf4j
public class BrowseRunner {

    public List<String> run(OpcUaClient opcUaClient) {
        List<String> nodesList = new ArrayList<>();
        try {
            List<? extends UaNode> nodes = opcUaClient.getAddressSpace().browseNodes(Identifiers.ObjectsFolder);

            nodesList.addAll(nodes.stream().filter(item -> !Objects.requireNonNull(item.getBrowseName().getName()).startsWith("_")
                            && Objects.equals(item.getBrowseName().getNamespaceIndex(), UShort.valueOf(2)))
                    .map(item -> item.getBrowseName().getName()).collect(Collectors.toList()));
        } catch (UaException e) {
            log.error("遍历根节点异常：{}", e.getMessage(), e);
        }
        return nodesList;
    }
}
