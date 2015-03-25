package org.garrit.executor;

import io.dropwizard.Configuration;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExecutorConfiguration extends Configuration
{
    private String name;
}