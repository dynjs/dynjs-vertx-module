package org.dynjs.vertx;

import org.dynjs.runtime.*;

import java.io.*;

public class VertxLoad extends AbstractNativeFunction {


    public VertxLoad(GlobalObject globalObject) {
        super(globalObject);
    }

    protected Object loadScript(ExecutionContext context, String scriptName)
            throws FileNotFoundException {
        if (scriptName == null) {
            return null;
        }
        File scriptFile = new File(scriptName);
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(context.getGlobalObject().getRuntime().getConfig().getClassLoader());
        Object ret = null;
        Runner runner = context.getGlobalObject().getRuntime().newRunner();
        try {
            LexicalEnvironment localEnv = context.getVariableEnvironment();
            localEnv.getRecord().createMutableBinding(context, "__vertxload", false);
            localEnv.getRecord().setMutableBinding(context, "__vertxload", scriptFile.getName(), false);

            if (scriptFile.exists()) {
                // TODO: This should not be added to the global object
                context.getGlobalObject().addLoadPath(scriptFile.getParent());
                ret = runner.withContext(context).withSource(scriptFile).execute();
            } else {
                InputStream is = context.getGlobalObject().getRuntime().getConfig().getClassLoader().getResourceAsStream(scriptName);
                if (is == null) {
                    throw new FileNotFoundException("Cannot find script: " + scriptName);
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                ret = runner.withContext(context).withSource(reader).execute();
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading script: " + scriptName + ". " + e.getLocalizedMessage());
            throw e;
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
        return ret;
    }

    @Override
    public Object call(ExecutionContext context, Object self, Object... args) {
        try {
            return loadScript(context, (String) args[0]);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
