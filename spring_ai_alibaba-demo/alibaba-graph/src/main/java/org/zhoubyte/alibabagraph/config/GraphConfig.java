package org.zhoubyte.alibabagraph.config;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class GraphConfig {

    Logger logger = LoggerFactory.getLogger(GraphConfig.class);

    @Bean("quickStartGraph")
    public CompiledGraph quickStartGraph() throws GraphStateException {
        // 定义状态图
        StateGraph quickStartGraph = new StateGraph("quickStartGraph", () -> {
            logger.info("quickStartGraph");
            return Map.of(
                    "input", new ReplaceStrategy(),
                    "output", new ReplaceStrategy()
            );
        });

        // 定义节点
        quickStartGraph.addNode("node1", AsyncNodeAction.node_async(state -> {
            logger.info("node1 = {}", state.toString());
            return Map.of(
                    "input", "graphConfig_addNode",
                    "output", "graphConfig_output"
            );
        }));

        quickStartGraph.addNode("node2", AsyncNodeAction.node_async(state -> {
            logger.info("node2 = {}", state.toString());
            return Map.of(
                    "input", "ZhouByte",
                    "output", "EMPTY"
            );
        }));

        // 定义边
        quickStartGraph.addEdge(StateGraph.START, "node1")
                .addEdge("node1", "node2")
                .addEdge("node2", StateGraph.END);
        return quickStartGraph.compile();
    }
}
