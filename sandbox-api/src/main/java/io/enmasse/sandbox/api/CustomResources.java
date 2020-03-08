package io.enmasse.sandbox.api;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionBuilder;

public class CustomResources {
    private CustomResources() {}

    public static CustomResourceDefinition createCustomResource(final String group, final String version, final String kind) {
        String singular = kind.toLowerCase();
        String listKind = kind + "List";
        String plural = singular + "s";
        if (singular.endsWith("s")) {
            plural = singular + "es";
        } else if (singular.endsWith("y")) {
            plural = singular.substring(0, singular.length() - 1) + "ies";
        }
        return new CustomResourceDefinitionBuilder()
                .editOrNewMetadata()
                .withName(plural + "." + group)
                .endMetadata()
                .editOrNewSpec()
                .withGroup(group)
                .withVersion(version)
                .withScope("Cluster")
                .editOrNewNames()
                .withKind(kind)
                .withListKind(listKind)
                .withPlural(plural)
                .withSingular(singular)
                .endNames()
                .endSpec()
                .build();
    }
}
