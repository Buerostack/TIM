package buerostack.introspection.service;

import buerostack.introspection.dto.IntrospectionResponse;

public interface TokenValidator {

    IntrospectionResponse introspect(String token);

    String getTokenType();
}