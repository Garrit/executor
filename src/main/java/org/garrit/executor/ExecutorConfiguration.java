package org.garrit.executor;

import io.dropwizard.Configuration;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExecutorConfiguration extends Configuration
{
    private String name;
    private URI negotiator;
    private Path problems;

    private HashMap<String, String> executors;
}