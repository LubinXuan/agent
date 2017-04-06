package com.adtime.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by xuanlubin on 2017/4/5.
 */
public class LogService {

    private static final Logger logger = LoggerFactory.getLogger(LogService.class);

    private static TraceServer traceServer;

    public static void setTraceServer(TraceServer traceServer) {
        LogService.traceServer = traceServer;
    }

    public static Invocation trace(String service, String method) {
        return trace(service, method, null);
    }

    public static Invocation trace(String service, String method, Object[] params) {
        TraceContext tc = TraceContext.getContext();
        if (null == tc) {
            return null;
        }
        tc.setTraceDepth(tc.getTraceDepth() + 1);
        logger.info("traceId:{} service:{} method:{} depth:{}", tc.getTraceId(), service, method,tc.getTraceDepth());
        Invocation invocation = new Invocation();
        invocation.setService(service);
        invocation.setMethod(method);
        invocation.setParams(params);
        invocation.setStart(System.currentTimeMillis());
        tc.getInvocationTrace().add(invocation);
        return invocation;
    }

    public static void traceError(Invocation invocation, Throwable throwable) {
        if (null == invocation) {
            return;
        }
        invocation.setThrowable(throwable);
    }

    public static void traceDone(Invocation invocation) {
        if (null == invocation) {
            logger.info("没有trace信息");
            return;
        }
        invocation.setEnd(System.currentTimeMillis());
        TraceContext tc = TraceContext.getContext();
        tc.setTraceDepth(tc.getTraceDepth() - 1);
        logger.info("traceId:{} service:{} method:{}  depth:{}", tc.getTraceId(), invocation.getService(), invocation.getMethod(), tc.getTraceDepth());
        if (tc.getTraceDepth() == 0) {
            //todo send trace info to trace server
            if (null != traceServer) {
                traceServer.reportTrace(tc);
            }
            TraceContext.destroy();
        }
    }
}
