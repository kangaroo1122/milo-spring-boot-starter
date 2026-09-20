package com.kangaroohy.milo.runner;

import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.NodeIds;
import org.eclipse.milo.opcua.stack.core.UaException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @since 2020/4/14
 */
public class BrowseRunner {

    /**
     * 遍历 ObjectsFolder 下的直接子节点。
     *
     * @param opcUaClient OPC UA 客户端
     * @return 根节点列表
     */
    public List<String> run(OpcUaClient opcUaClient) throws UaException {
        List<String> nodesList = new ArrayList<>();
        List<? extends UaNode> nodes = opcUaClient.getAddressSpace().browseNodes(NodeIds.ObjectsFolder);

        nodesList.addAll(nodes.stream().filter(item -> item.getBrowseName() != null
                        && item.getBrowseName().getName() != null
                        && !item.getBrowseName().getName().startsWith("_"))
                .map(item -> item.getBrowseName().getName()).collect(Collectors.toList()));
        return nodesList;
    }
}
