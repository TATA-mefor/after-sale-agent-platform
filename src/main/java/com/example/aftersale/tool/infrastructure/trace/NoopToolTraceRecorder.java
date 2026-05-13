package com.example.aftersale.tool.infrastructure.trace;

import com.example.aftersale.tool.application.ToolTraceRecord;
import com.example.aftersale.tool.application.ToolTraceRecorder;
import org.springframework.stereotype.Component;

@Component
public class NoopToolTraceRecorder implements ToolTraceRecorder {

    @Override
    public void record(ToolTraceRecord record) {
    }
}
