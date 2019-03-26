module com.theoryinpractise.halbuilder.json {
    exports com.theoryinpractise.halbuilder.json;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.google.common;
    requires transitive com.theoryinpractise.halbuilder.api;
    requires transitive com.theoryinpractise.halbuilder.core;
}
