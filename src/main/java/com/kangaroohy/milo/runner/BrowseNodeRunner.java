package com.kangaroohy.milo.runner;

import com.kangaroohy.milo.utils.CustomUtil;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author kangaroo hy
 * @version 0.0.1
 * @since 2020/4/14
 */
public class BrowseNodeRunner {
    /**
     * 要读的节点
     */
    private final String browseRoot;

    public BrowseNodeRunner(String browseRoot) {
        this.browseRoot = browseRoot;
    }

    /**
     * 递归遍历指定节点，并返回叶子节点路径。
     *
     * @param opcUaClient OPC UA 客户端
     * @return 叶子节点路径列表
     */
    public List<String> run(OpcUaClient opcUaClient) throws UaException {
        NodeId nodeId = CustomUtil.parseNodeId(browseRoot);
        return browseNode(browseRoot, opcUaClient, nodeId, new HashSet<>());
    }

    private List<String> browseNode(String prefix, OpcUaClient client, NodeId browseRoot,
                                    Set<NodeId> visited) throws UaException {
        List<String> nodesList = new ArrayList<>();
        if (!visited.add(browseRoot)) {
            return nodesList;
        }
        try {
            List<? extends UaNode> nodes = client.getAddressSpace().browseNodes(browseRoot);

            nodes = nodes.stream().filter(item -> item.getBrowseName() != null
                    && item.getBrowseName().getName() != null
                    && !item.getBrowseName().getName().startsWith("_"))
                    .collect(Collectors.toList());

            for (UaNode node : nodes) {
                String sub = prefix + "." + node.getBrowseName().getName();

                // Recursion protection is path-local. OPC UA address spaces can
                // legitimately expose the same node from more than one branch.
                if (visited.contains(node.getNodeId())) {
                    continue;
                }
                List<String> browseNode = browseNode(sub, client, node.getNodeId(), visited);
                if (browseNode.isEmpty()) {
                    nodesList.add(sub);
                } else {
                    nodesList.addAll(browseNode);
                }
            }
            return nodesList;
        } finally {
            visited.remove(browseRoot);
        }
    }
}
