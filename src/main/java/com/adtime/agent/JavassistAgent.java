package com.adtime.agent;

import javassist.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.*;

/**
 * Created by xuanlubin on 2017/4/5.
 */
public class JavassistAgent implements ClassFileTransformer {

    private static final Logger logger = LoggerFactory.getLogger(JavassistAgent.class);

    private static final Map<ClassLoader, ClassPool> POOL_MAP = Collections.synchronizedMap(new WeakHashMap<ClassLoader, ClassPool>());

    private String traceApplication;

    JavassistAgent(String traceApplication) {
        this.traceApplication = traceApplication.replace(".", "/");
        logger.info("traceApplication:{}", this.traceApplication);
    }

    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {

        if (className.startsWith(traceApplication)) {
            try {
                CtClass cc = getCtClass(classfileBuffer, loader);
                if (!cc.hasAnnotation(TraceClass.class)) {
                    return classfileBuffer;
                } else {
                    CtMethod ctMethods[] = cc.getDeclaredMethods();
                    if (null != ctMethods) {
                        for (CtMethod ctMethod : ctMethods) {
                            if (ctMethod.hasAnnotation(TraceMethod.class)) {
                                logger.info("traceMethod:{}", ctMethod.getLongName());
                                trace(cc, ctMethod);
                            }
                        }
                    }
                    return cc.toBytecode();
                }
            } catch (ClassNotFoundException | CannotCompileException | IOException | NotFoundException e) {
                e.printStackTrace();
            }
        }

        return classfileBuffer;
    }


    private void trace(CtClass ctClass, CtMethod ctMethod) throws CannotCompileException, ClassNotFoundException, NotFoundException {
        Object[][] annotations = ctMethod.getParameterAnnotations();
        CtClass[] paramClasses = ctMethod.getParameterTypes();
        List<String> traceParam = new ArrayList<String>(16);
        if (null != annotations) {
            for (int i = 0; i < annotations.length; i++) {
                Object[] annos = annotations[i];
                for (Object anno : annos) {
                    if (anno instanceof TraceParam) {
                        CtClass paramClass = paramClasses[i];
                        if (paramClass.isPrimitive()) {
                            traceParam.add("com.adtime.agent.PrimitiveBox.box($" + (i + 1) + ")");
                        } else {
                            traceParam.add("$" + (i + 1));
                        }
                        break;
                    }
                }
            }
        }

        CtMethod newMethod = CtNewMethod.copy(ctMethod, ctClass, null);

        String oldName = "$" + ctMethod.getName();
        ctMethod.setName(oldName);
        ctMethod.setModifiers(Modifier.PRIVATE);

        StringBuilder src = new StringBuilder();
        src.append("{com.adtime.agent.Invocation $$inv = com.adtime.agent.LogService.trace(\"");
        src.append(ctClass.getName()).append("\",\"").append(newMethod.getName()).append("\"");
        if (!traceParam.isEmpty()) {
            src.append(",").append("new java.lang.Object[]{").append(String.join(",", traceParam)).append("}");
        }
        src.append(");");
        src.append("try{").append("void".equals(newMethod.getReturnType().getName()) ? "" : "return ").append(oldName).append("($$);");
        src.append("}catch(Throwable $$e){com.adtime.agent.LogService.traceError($$inv,$$e);throw $$e;}finally{com.adtime.agent.LogService.traceDone($$inv);}}");
        newMethod.setBody(src.toString());
        ctClass.addMethod(newMethod);
    }

    private CtClass getCtClass(byte[] classFileBuffer, ClassLoader classLoader) throws IOException {
        ClassPool classPool = getClassPool(classLoader);
        CtClass clazz = classPool.makeClass(new ByteArrayInputStream(classFileBuffer), false);
        clazz.defrost();
        return clazz;
    }

    private static ClassPool getClassPool(ClassLoader loader) {
        if (loader == null)
            return ClassPool.getDefault();

        ClassPool pool = POOL_MAP.get(loader);
        if (pool == null) {
            pool = new ClassPool(true);
            pool.appendClassPath(new LoaderClassPath(loader));
            POOL_MAP.put(loader, pool);
        }
        return pool;
    }


}
