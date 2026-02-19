package com.darkbladedev.cee.core.definition;

public sealed interface FlowNodeDefinition permits ActionNodeDefinition, DelayNodeDefinition, RepeatNodeDefinition, ParallelNodeDefinition {
}
