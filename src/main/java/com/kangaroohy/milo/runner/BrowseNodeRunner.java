package com.kangaroohy.milo.runner;

import com.kangaroohy.milo.utils.CustomUtil;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.sdk.client.nodes.UaNode;
import org.eclipse.milo.opcua.stack.core.UaException;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;

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
public class BrowseNodeRunner {
    /**
     * 要读的节点
     */
    private final String browseRoot;

    public BrowseNodeRunner(String browseRoot) {
        this.browseRoot = browseRoot;
    }

    public List<String> run(OpcUaClient opcUaClient) {
        NodeId nodeId = CustomUtil.parseNodeId(browseRoot);
        return browseNode(browseRoot, opcUaClient, nodeId);
    }

    private List<String> browseNode(String prefix, OpcUaClient client, NodeId browseRoot) {
        List<String> nodesList = new ArrayList<>();
        try {
            List<? extends UaNode> nodes = client.getAddressSpace().browseNodes(browseRoot);

            nodes = nodes.stream().filter(item -> !Objects.requireNonNull(item.getBrowseName().getName()).startsWith("_")).collect(Collectors.toList());

            for (UaNode node : nodes) {
                String sub = prefix + "." + node.getBrowseName().getName();

                // recursively browse to children
                List<String> browseNode = browseNode(sub, client, node.getNodeId());
                if (browseNode.isEmpty()) {
                    nodesList.add(sub);
                } else {
                    nodesList.addAll(browseNode(sub, client, node.getNodeId()));
                }
            }
        } catch (UaException e) {
            log.error("Browsing nodeId={} failed: {}", browseRoot, e.getMessage(), e);
        }
        return nodesList;
    }
}
