package com.adtime.agent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

/**
 * Created by xuanlubin on 2017/4/5.
 */
public class TraceContext {
    private static final ThreadLocal<TraceContext> TRACE_CONTEXT = new ThreadLocal<TraceContext>();

    private static final ThreadLocal<String> TRACE_ID_CONTEXT = new ThreadLocal<String>();

    private static final ThreadLocal<DateFormat> DATE_FORMAT_THREAD_LOCAL = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyyMMddHHmm");
        }
    };

    public static void startTrace() {
        if (null != TRACE_CONTEXT.get()) {
            return;
        }
        String traceId = DATE_FORMAT_THREAD_LOCAL.get().format(new Date()) + "-" + UUID.randomUUID().toString();
        startTrace(traceId);
    }

    public static void startTrace(String traceId) {
        TRACE_ID_CONTEXT.set(traceId);
        TRACE_CONTEXT.set(new TraceContext(traceId));
    }

    public static TraceContext getContext() {
        startTrace();
        return TRACE_CONTEXT.get();
    }

    public static void destroy() {
        TRACE_CONTEXT.remove();
        TRACE_ID_CONTEXT.remove();
    }

    private String traceId;

    private long startTime;

    private int traceDepth;

    private List<Invocation> invocationTrace;

    public TraceContext(String traceId) {
        this.startTime = System.currentTimeMillis();
        this.traceId = traceId;
        this.invocationTrace = new LinkedList<Invocation>();
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public List<Invocation> getInvocationTrace() {
        return invocationTrace;
    }

    public void setInvocationTrace(List<Invocation> invocationTrace) {
        this.invocationTrace = invocationTrace;
    }

    public int getTraceDepth() {
        return traceDepth;
    }

    public void setTraceDepth(int traceDepth) {
        this.traceDepth = traceDepth;
    }
}
