package com.example.aftersale.tool.application;

import com.example.aftersale.tool.domain.ToolDefinition;
import com.example.aftersale.tool.domain.ToolInput;
import com.example.aftersale.tool.domain.ToolOutput;

public interface ToolExecutor {

    ToolDefinition definition();

    ToolOutput execute(ToolInput input);
}
