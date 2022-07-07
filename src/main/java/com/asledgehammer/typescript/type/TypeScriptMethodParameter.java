package com.asledgehammer.typescript.type;

import com.asledgehammer.typescript.TypeScriptGraph;
import com.asledgehammer.typescript.util.ClazzUtils;
import com.asledgehammer.typescript.util.ComplexGenericMap;

import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

@SuppressWarnings("unused")
public class TypeScriptMethodParameter
        implements TypeScriptWalkable, TypeScriptCompilable {

    private final TypeScriptMethod method;
    private final Type type;
    final String name;
    private final Parameter parameter;
    final Type parameterizedType;
    private boolean walked = false;
    private String returnType;

    TypeScriptMethodParameter(TypeScriptMethod method, Parameter parameter, Type type) {
        this.method = method;
        this.parameter = parameter;
        this.type = type;
        this.name = parameter.getName();
        this.parameterizedType = parameter.getParameterizedType();
    }

    @Override
    public void walk(TypeScriptGraph graph) {
        ComplexGenericMap genericMap = this.method.getContainer().genericMap;
        if (genericMap != null) {
            Class<?> declClazz = method.getMethod().getDeclaringClass();
            String before = type.getTypeName();
            this.returnType = ClazzUtils.walkTypesRecursively(genericMap, declClazz, before);
        } else {
            this.returnType = type.getTypeName();
        }

        this.returnType = TypeScriptElement.inspect(graph, this.returnType);
        this.returnType = TypeScriptElement.adaptType(this.returnType);

        graph.add(parameter.getType());

        // Add any missing parameters if not defined.
        if (!returnType.contains("<")) {
            TypeVariable<?>[] params = parameter.getType().getTypeParameters();
            if (params.length != 0) {
                returnType += "<";
                for (int i = 0; i < params.length; i++) {
                    returnType += "any, ";
                }
                returnType = returnType.substring(0, returnType.length() - 2) + ">";
            }
        }

        if (!parameter.getType().isPrimitive()) {
            returnType += " | null";
        }

        walked = true;
    }

    @Override
    public String compile(String prefix) {
        String name = this.name;
        return name + (parameter.isVarArgs() ? "?" : "") + ": " + returnType;
    }

    public boolean hasWalked() {
        return walked;
    }
}
