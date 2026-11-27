package bg.petarh.piano.api.rpc;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RpcServer {

    private static final String VERSION = "2.0";

    private record Registered<P>(Class<P> paramsType, RpcMethod<P> method) {
    }

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final Map<String, Registered<?>> methods = new ConcurrentHashMap<>();

    public RpcServer() {
    }

    public <P> void register(String name, Class<P> paramsType, RpcMethod<P> method) {
        methods.put(name, new Registered<>(paramsType, method));
    }

    public Optional<String> handle(String line) {
        JsonNode request;
        try {
            request = mapper.readTree(line);
        } catch (JacksonException e) {
            return Optional.of(error(null, RpcException.PARSE_ERROR, "Parse error: " + e.getOriginalMessage()));
        }
        if (!request.isObject()) {
            return Optional.of(error(null, RpcException.INVALID_REQUEST, "Invalid request: not an object"));
        }
        JsonNode id = request.get("id");
        JsonNode method = request.get("method");
        if (method == null || !method.isString()) {
            return Optional.of(error(id, RpcException.INVALID_REQUEST, "Invalid request: no method"));
        }
        String response = call(id, method.asString(), request.get("params"));
        return id == null ? Optional.empty() : Optional.of(response);
    }

    public String notification(String method, Object params) {
        ObjectNode message = mapper.createObjectNode();
        message.put("jsonrpc", VERSION);
        message.put("method", method);
        message.set("params", mapper.valueToTree(params));
        return mapper.writeValueAsString(message);
    }

    private String call(JsonNode id, String name, JsonNode params) {
        Registered<?> registered = methods.get(name);
        if (registered == null) {
            return error(id, RpcException.METHOD_NOT_FOUND, "Method not found: " + name);
        }
        try {
            return result(id, invoke(registered, params));
        } catch (RpcException e) {
            return error(id, e.code(), e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(id, RpcException.INVALID_PARAMS, e.getMessage());
        } catch (RuntimeException e) {
            return error(id, RpcException.INTERNAL_ERROR, "Internal error: " + e.getMessage());
        }
    }

    private <P> Object invoke(Registered<P> registered, JsonNode params) {
        P converted;
        try {
            converted = registered.paramsType() == Void.class || params == null
                    ? null
                    : mapper.treeToValue(params, registered.paramsType());
        } catch (JacksonException e) {
            throw new RpcException(RpcException.INVALID_PARAMS, "Invalid params: " + e.getOriginalMessage());
        }
        return registered.method().call(converted);
    }

    private String result(JsonNode id, Object value) {
        ObjectNode response = envelope(id);
        response.set("result", mapper.valueToTree(value));
        return mapper.writeValueAsString(response);
    }

    private String error(JsonNode id, int code, String message) {
        ObjectNode response = envelope(id);
        ObjectNode error = response.putObject("error");
        error.put("code", code);
        error.put("message", message);
        return mapper.writeValueAsString(response);
    }

    private ObjectNode envelope(JsonNode id) {
        ObjectNode response = mapper.createObjectNode();
        response.put("jsonrpc", VERSION);
        if (id == null) {
            response.putNull("id");
        } else {
            response.set("id", id);
        }
        return response;
    }
}
