package com.darkbladedev.cee.core.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import com.darkbladedev.cee.api.Action;
import com.darkbladedev.cee.core.definition.ActionNodeDefinition;
import com.darkbladedev.cee.core.definition.DelayNodeDefinition;
import com.darkbladedev.cee.core.definition.FlowDefinition;
import com.darkbladedev.cee.core.definition.FlowNodeDefinition;
import com.darkbladedev.cee.core.definition.AsyncNodeDefinition;
import com.darkbladedev.cee.core.definition.RepeatNodeDefinition;

public final class FlowCompiler {
    private final Function<ActionNodeDefinition, Action> actionFactory;

    public FlowCompiler(Function<ActionNodeDefinition, Action> actionFactory) {
        this.actionFactory = Objects.requireNonNull(actionFactory, "actionFactory");
    }

    public ExecutionPlan compile(FlowDefinition definition) {
        List<Instruction> instructions = new ArrayList<>();
        compileNodes(definition.getNodes(), instructions);
        return new ExecutionPlan(instructions);
    }

    private void compileNodes(List<FlowNodeDefinition> nodes, List<Instruction> instructions) {
        for (FlowNodeDefinition node : nodes) {
            if (node instanceof ActionNodeDefinition actionNode) {
                instructions.add(new ActionInstruction(actionFactory.apply(actionNode)));
            } else if (node instanceof DelayNodeDefinition delayNode) {
                instructions.add(new DelayInstruction(delayNode.getTicks()));
            } else if (node instanceof RepeatNodeDefinition repeatNode) {
                int loopStartIndex = instructions.size();
                instructions.add(new LoopStartInstruction(repeatNode.getTimes()));
                compileNodes(repeatNode.getFlow().getNodes(), instructions);
                instructions.add(new LoopEndInstruction(loopStartIndex, repeatNode.getEveryTicks()));
            } else if (node instanceof AsyncNodeDefinition asyncNode) {
                List<ExecutionPlan> branches = new ArrayList<>();
                for (FlowDefinition branch : asyncNode.getBranches()) {
                    branches.add(compile(branch));
                }
                instructions.add(new AsyncInstruction(branches));
            }
        }
    }
}
