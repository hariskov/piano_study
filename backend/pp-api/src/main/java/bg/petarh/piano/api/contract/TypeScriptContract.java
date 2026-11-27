package bg.petarh.piano.api.contract;

import bg.petarh.piano.api.rpc.ApiEvent;
import bg.petarh.piano.api.rpc.MayBeAbsent;
import bg.petarh.piano.api.rpc.MethodSignature;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class TypeScriptContract {

    private static final String HEADER = """
            // Generated from the Java API records (pp-api). Do not edit by hand: change the records, then run
            // ./mvnw -pl backend/pp-api -am test -Dprotocol.write=true to write this file again.
            """;
    private static final Set<Class<?>> NUMBERS = Set.of(int.class, Integer.class, long.class, Long.class,
            double.class, Double.class, float.class, Float.class);

    private final Map<String, String> interfaces = new TreeMap<>();
    private final Map<String, Class<?>> named = new TreeMap<>();

    private TypeScriptContract() {
    }

    public static String generate(List<MethodSignature> methods, List<ApiEvent<?>> events) {
        return new TypeScriptContract().write(methods, events);
    }

    private String write(List<MethodSignature> methods, List<ApiEvent<?>> events) {
        StringBuilder methodMap = new StringBuilder("export interface ApiMethods {\n");
        for (MethodSignature method : methods) {
            methodMap.append("  \"").append(method.name()).append("\": { params: ")
                    .append(params(method.paramsType())).append("; result: ")
                    .append(result(method.resultType())).append(" };\n");
        }
        methodMap.append("}\n");
        StringBuilder eventMap = new StringBuilder("export interface ApiEvents {\n");
        events.stream().sorted(Comparator.comparing(ApiEvent::name)).forEach(event -> eventMap
                .append("  \"").append(event.name()).append("\": ")
                .append(type(event.paramsType(), false)).append(";\n"));
        eventMap.append("}\n");
        StringBuilder output = new StringBuilder(HEADER);
        interfaces.values().forEach(definition -> output.append('\n').append(definition));
        return output.append('\n').append(methodMap).append('\n').append(eventMap).toString();
    }

    private String params(Class<?> type) {
        return type == Void.class ? "Record<string, never>" : type(type, true);
    }

    private String result(Class<?> type) {
        return type == Void.class ? "null" : type(type, false);
    }

    private String type(Type type, boolean allOptional) {
        if (type instanceof ParameterizedType parameterized) {
            Class<?> raw = (Class<?>) parameterized.getRawType();
            Type[] arguments = parameterized.getActualTypeArguments();
            if (Collection.class.isAssignableFrom(raw)) {
                return type(arguments[0], allOptional) + "[]";
            }
            if (Map.class.isAssignableFrom(raw)) {
                return "Record<string, " + type(arguments[1], allOptional) + ">";
            }
            throw new IllegalArgumentException("No TypeScript type for " + type);
        }
        Class<?> plain = (Class<?>) type;
        if (plain == String.class) {
            return "string";
        }
        if (NUMBERS.contains(plain)) {
            return "number";
        }
        if (plain == boolean.class || plain == Boolean.class) {
            return "boolean";
        }
        if (plain.isRecord()) {
            return declare(plain, allOptional);
        }
        throw new IllegalArgumentException("No TypeScript type for " + type);
    }

    private String declare(Class<?> record, boolean allOptional) {
        String name = record.getSimpleName();
        Class<?> existing = named.putIfAbsent(name, record);
        if (existing != null && existing != record) {
            throw new IllegalArgumentException("Two API records are called " + name + ": " + existing + ", " + record);
        }
        if (!interfaces.containsKey(name)) {
            interfaces.put(name, "");
            StringBuilder definition = new StringBuilder("export interface ").append(name).append(" {\n");
            for (RecordComponent component : record.getRecordComponents()) {
                boolean optional = allOptional || component.isAnnotationPresent(MayBeAbsent.class);
                definition.append("  ").append(component.getName()).append(optional ? "?: " : ": ")
                        .append(type(component.getGenericType(), allOptional)).append(";\n");
            }
            interfaces.put(name, definition.append("}\n").toString());
        }
        return name;
    }
}
