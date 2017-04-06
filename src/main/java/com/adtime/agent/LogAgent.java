package com.adtime.agent;

import java.lang.instrument.Instrumentation;

/**
 * Created by xuanlubin on 2017/4/5.
 */
public class LogAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        if (null == agentArgs || agentArgs.trim().length() == 0) {
            System.out.println("没有找到trace配置文件");
            return;
        }

        LogService.setTraceServer(new TraceServer() {
            public void reportTrace(TraceContext tc) {
                StringBuilder sb = new StringBuilder();
                sb.append("traceId:").append(tc.getTraceId()).append("\r\n");
                sb.append("startTime:").append(tc.getStartTime()).append("\r\n");
                sb.append("trace:").append("\r\n");
                for (int i = 0; i < tc.getInvocationTrace().size(); i++) {
                    for (int j = -1; j < i; j++) {
                        sb.append("\t");
                    }
                    Invocation inv = tc.getInvocationTrace().get(i);
                    sb.append(inv.getService()).append("#").append(inv.getMethod());
                    sb.append("\tcost:").append(inv.getEnd() - inv.getStart());
                    sb.append("\r\n");
                }
                System.out.println(sb.toString());
            }
        });

        inst.addTransformer(new JavassistAgent(agentArgs.trim()));
    }
}
