package org.zhoubyte.alibabagraph.config;


import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping(value = "/v1")
public class GraphController {

    @Resource
    private CompiledGraph quickStartGraph;

    @GetMapping(value = "/graph")
    public Flux<String> startGraph() {
        // 使用响应式返回结果，避免在 WebFlux I/O 线程中调用 block()/blockFirst()
        return quickStartGraph.stream(Map.of())
                .map(NodeOutput::toString);
    }
}
