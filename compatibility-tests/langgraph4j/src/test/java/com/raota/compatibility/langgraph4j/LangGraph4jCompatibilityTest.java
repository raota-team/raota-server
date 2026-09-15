package com.raota.compatibility.langgraph4j;

import static org.assertj.core.api.Assertions.assertThat;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class LangGraph4jCompatibilityTest {

    @Test
    void runsLoadNormalizeValidateGraphAndPreservesStateJsonValues() throws Exception {
        var graph = graph().compile();
        WorkflowState last = graph.invoke(Map.of(WorkflowState.INPUT, "  라멘  ")).orElseThrow();
        assertThat(last.value(WorkflowState.NORMALIZED)).contains("라멘");
        assertThat(last.value(WorkflowState.VALID)).contains(true);
        assertThat(last.value(WorkflowState.JSON_ROUND_TRIP)).contains("{\"valid\":true}");
    }

    @Test
    void routesInvalidInputToInvalidNodeAndPropagatesFailureState() throws Exception {
        var graph = graph().compile();
        WorkflowState last = graph.invoke(Map.of(WorkflowState.INPUT, "   ")).orElseThrow();
        assertThat(last.value(WorkflowState.VALID)).contains(false);
        assertThat(last.value(WorkflowState.ERROR)).contains("input is blank");
    }

    @Test
    void springAiModuleIsOnTheSameClasspathAsCore() throws ClassNotFoundException {
        assertThat(Class.forName("org.bsc.langgraph4j.spring.ai.serializer.std.SpringAIStateSerializer")).isNotNull();
    }

    @Test
    void checkpointsStateByThreadAndCanReadTheLastSnapshot() throws Exception {
        MemorySaver saver = new MemorySaver();
        var graph = graph().compile(CompileConfig.builder().graphId("compatibility").releaseThread(false).checkpointSaver(saver).build());
        RunnableConfig config = RunnableConfig.builder().graphId("compatibility").threadId("compatibility-thread").build();

        WorkflowState state = graph.invoke(Map.of(WorkflowState.INPUT, " ramen "), config).orElseThrow();

        assertThat(state.value(WorkflowState.ROUTE)).contains("valid");
        assertThat(saver.list(config)).isNotEmpty();
        assertThat(graph.lastStateOf(config)).isPresent();
    }

    @Test
    void springContextInjectsAWorkflowNode() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(BeanConfig.class)) {
            assertThat(context.getBean(WorkflowNode.class).name()).isEqualTo("validate");
        }
    }

    @Test
    void usesDeterministicFixedChatModelInsteadOfAnExternalLlm() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(BeanConfig.class)) {
            ChatResponse response = context.getBean(ChatModel.class).call(new Prompt("평가"));

            assertThat(response.getResult().getOutput().getText()).isEqualTo("{\"valid\":true}");
        }
    }

    private StateGraph<WorkflowState> graph() throws Exception {
        NodeAction<WorkflowState> load = state -> Map.of(
                WorkflowState.NORMALIZED,
                state.<String>value(WorkflowState.INPUT).orElse("").trim()
        );
        NodeAction<WorkflowState> normalize = state -> Map.of(
                WorkflowState.NORMALIZED,
                state.<String>value(WorkflowState.NORMALIZED).orElse("").toLowerCase()
        );
        NodeAction<WorkflowState> validate = state -> {
            String value = state.<String>value(WorkflowState.NORMALIZED).orElse("");
            boolean valid = !value.isBlank();
            return Map.of(
                    WorkflowState.VALID, valid,
                    WorkflowState.ERROR, valid ? "" : "input is blank",
                    WorkflowState.JSON_ROUND_TRIP, valid ? "{\"valid\":true}" : "{\"valid\":false}"
            );
        };
        NodeAction<WorkflowState> valid = state -> Map.of(WorkflowState.ROUTE, "valid");
        NodeAction<WorkflowState> invalid = state -> Map.of(WorkflowState.ROUTE, "invalid");

        return new StateGraph<>(WorkflowState.SCHEMA, WorkflowState::new)
                .addNode("load", node_async(load))
                .addNode("normalize", node_async(normalize))
                .addNode("validate", node_async(validate))
                .addNode("valid", node_async(valid))
                .addNode("invalid", node_async(invalid))
                .addEdge(START, "load")
                .addEdge("load", "normalize")
                .addEdge("normalize", "validate")
                .addConditionalEdges("validate", edge_async(state -> state.<Boolean>value(WorkflowState.VALID).orElse(false) ? "valid" : "invalid"),
                        Map.of("valid", "valid", "invalid", "invalid"))
                .addEdge("valid", END)
                .addEdge("invalid", END);
    }

    static final class WorkflowState extends AgentState {
        static final String INPUT = "input";
        static final String NORMALIZED = "normalized";
        static final String VALID = "valid";
        static final String ERROR = "error";
        static final String ROUTE = "route";
        static final String JSON_ROUND_TRIP = "jsonRoundTrip";
        static final Map<String, Channel<?>> SCHEMA = Map.of(
                INPUT, Channels.base(() -> ""),
                NORMALIZED, Channels.base(() -> ""),
                VALID, Channels.base(() -> false),
                ERROR, Channels.base(() -> ""),
                ROUTE, Channels.base(() -> ""),
                JSON_ROUND_TRIP, Channels.base(() -> "")
        );

        WorkflowState(Map<String, Object> initData) {
            super(initData);
        }
    }

    static final class WorkflowNode {
        String name() {
            return "validate";
        }
    }

    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class BeanConfig {
        @org.springframework.context.annotation.Bean
        WorkflowNode workflowNode() {
            return new WorkflowNode();
        }

        @org.springframework.context.annotation.Bean
        ChatModel fixedChatModel() {
            return new ChatModel() {
                @Override
                public ChatResponse call(Prompt prompt) {
                    return new ChatResponse(List.of(new Generation(new AssistantMessage("{\"valid\":true}"))));
                }
            };
        }
    }
}
