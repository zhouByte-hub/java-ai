package org.zhoubyte.alibabagraph.config;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.Optional;

@Configuration
public class GraphConfig {

    Logger logger = LoggerFactory.getLogger(GraphConfig.class);

    @Bean("quickStartGraph")
    public CompiledGraph quickStartGraph() throws GraphStateException {
        // 定义状态图
        StateGraph quickStartGraph = new StateGraph("quickStartGraph", () -> {
            logger.info("quickStartGraph");
            /* 值策略
                1. ReplaceStrategy：替换，新值代替老值。
                2. AppendStrategy：追加，适合 List 类型的数据，新的 List 数据会追加到老 List 对象中。
                3. MergeStrategy：合并，合适 Map 类型的数据，新老 Map 数据会合并
             */
            return Map.of(
                    "input", new ReplaceStrategy(),
                    "output", new ReplaceStrategy()
            );
        });

        // 定义节点
        /*
            NodeAction：是 Graph中对节点的抽象，我们只需要实现NodeAction接口，在apply方法中定义节点的执行逻辑就可以了。
            AsyncNodeAction：异步节点，提供了一个静态方法可以将 NodeAction转换为AsyncNodeAction。
         */
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

        // 定义边（顺序边）
        quickStartGraph.addEdge(StateGraph.START, "node1")
                .addEdge("node1", "node2")
                .addEdge("node2", StateGraph.END);

        return quickStartGraph.compile();
    }


    // 条件边
    private void conditionEdge(StateGraph quickStartGraph) throws GraphStateException {
                /*
            addConditionalEdges 的3个参数含义：
            1) sourceId："node2"
               表示“从哪个节点出发做条件跳转”，也就是条件边的“源节点”。
            2) AsyncEdgeAction.edge_async(state -> { ... })
               - 这里把一个 EdgeAction（同步函数）包装成 AsyncEdgeAction（异步）。
               - 入参 state：OverAllState，当前图执行到 node2 时的“全局状态”，本质是一个带合并策略的 Map。
               - 返回值 String：一个“条件标签”，后面会拿这个标签去 mappings 中查找下一跳节点 ID。
                 当前代码中，当 state 中存在 key = "output" 时返回一个标签，
                 否则返回另一个标签（具体值以下面 lambda 体中的 return 为准），
                 这些标签必须能在 mappings 这个 Map 中找到对应的 key。
            3) mappings：Map<String, String>
               - 这里实际传入的是：
                 Map.of(
                     "A", "node1",
                     "B", StateGraph.END
                 );
               - key：上面 EdgeAction 返回的条件标签（例如 "A" 或 "B"）。
               - value：真正要跳转到的“目标节点 ID”，例如：
                 - "A" -> "node1"：条件命中 "A" 时，重新跳回 node1 再走一遍流程；
                 - "B" -> StateGraph.END：条件命中 "B" 时，直接跳转到 END 结束图执行。
               - 注意：根据 StateGraph 源码，如果 mappings 为空或为 null 会抛出 GraphStateException(edgeMappingIsEmpty)，
                 所以现在这种写法已经是“有效映射”了；如果你希望某个条件标签生效，
                 EdgeAction 中的返回值必须与这里的 key 对应（例如返回 "A" 或 "B"）。
            返回值：
            - 当 condition 返回的是 A 时，执行到 node1节点
            - 当 condition 返回的是 B 时，直接结束。
         */
        quickStartGraph.addConditionalEdges("node2", AsyncEdgeAction.edge_async(state -> {
            Optional<Object> output = state.value("output");
            if (output.isPresent()) {
                return "EMPTY";
            }
            return state.toString();
        }), Map.of(
                "A", "node1",
                "B", StateGraph.END
        ));
    }
}
