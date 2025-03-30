package org.tabooproject.fluxon.jsr223;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Fluxon 脚本引擎工厂
 * 用于创建 Fluxon 脚本引擎实例
 */
public class FluxonScriptEngineFactory implements ScriptEngineFactory {
    
    private static final String ENGINE_NAME = "fluxon";
    private static final String ENGINE_VERSION = "1.0";
    private static final String LANGUAGE_NAME = "Fluxon";
    private static final String LANGUAGE_VERSION = "1.0";
    
    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public String getEngineVersion() {
        return ENGINE_VERSION;
    }

    @Override
    public List<String> getExtensions() {
        return Collections.singletonList("flx");
    }

    @Override
    public List<String> getMimeTypes() {
        return Collections.singletonList("application/x-fluxon");
    }

    @Override
    public List<String> getNames() {
        return Arrays.asList(ENGINE_NAME, "Fluxon", "fluxon", "FLUXON");
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    @Override
    public String getLanguageVersion() {
        return LANGUAGE_VERSION;
    }

    @Override
    public Object getParameter(String key) {
        switch (key) {
            case ScriptEngine.NAME:
            case ScriptEngine.ENGINE:
                return getEngineName();
            case ScriptEngine.ENGINE_VERSION:
                return getEngineVersion();
            case ScriptEngine.LANGUAGE:
                return getLanguageName();
            case ScriptEngine.LANGUAGE_VERSION:
                return getLanguageVersion();
            case "THREADING":
                return "THREAD-ISOLATED";
            default:
                return null;
        }
    }

    @Override
    public String getMethodCallSyntax(String obj, String m, String... args) {
        StringBuilder sb = new StringBuilder();
        sb.append(obj).append(".").append(m).append("(");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(args[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public String getOutputStatement(String toDisplay) {
        return "print(" + toDisplay + ")";
    }

    @Override
    public String getProgram(String... statements) {
        StringBuilder sb = new StringBuilder();
        for (String statement : statements) {
            sb.append(statement).append(";\n");
        }
        return sb.toString();
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new FluxonScriptEngine(this);
    }
} 