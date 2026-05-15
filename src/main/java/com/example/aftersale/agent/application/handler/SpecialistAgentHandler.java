package com.example.aftersale.agent.application.handler;

import com.example.aftersale.agent.application.planner.SubtaskType;

public interface SpecialistAgentHandler {

    SubtaskType supportedType();

    SubtaskExecutionResult handle(SubtaskExecutionContext context);

    default boolean supports(SubtaskType type) {
        return supportedType() == type;
    }
}
