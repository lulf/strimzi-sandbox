/*
 * Copyright 2020, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package io.enmasse.sandbox.operator;

import io.fabric8.kubernetes.api.model.HasMetadata;

import java.util.Objects;

public class ObjectKey {
    private final String name;
    private final String namespace;

    public ObjectKey(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    public static ObjectKey fromMetadata(HasMetadata metadata) {
        return new ObjectKey(metadata.getMetadata().getName(), metadata.getMetadata().getNamespace() != null && metadata.getMetadata().getNamespace().isEmpty() ? null : metadata.getMetadata().getNamespace());
    }

    @Override
    public String toString() {
        return "ObjectKey{" +
                "name='" + name + '\'' +
                ", namespace='" + namespace + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObjectKey that = (ObjectKey) o;
        return name.equals(that.name) &&
                Objects.equals(namespace, that.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, namespace);
    }
}
