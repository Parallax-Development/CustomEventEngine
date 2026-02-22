package com.darkbladedev.cee.core.commands.impl;

import com.darkbladedev.cee.core.commands.ArgumentDefinition;
import com.darkbladedev.cee.core.commands.CommandContext;
import com.darkbladedev.cee.core.commands.SubCommand;
import com.darkbladedev.cee.core.definition.TaskDefinition;
import com.darkbladedev.cee.core.definition.TaskParameterDefinition;
import com.darkbladedev.cee.core.definition.TaskReturnDefinition;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TasksExportCommand implements SubCommand {
    private final CommandServices services;
    private final MessageService messages;

    public TasksExportCommand(CommandServices services, MessageService messages) {
        this.services = services;
        this.messages = messages;
    }

    @Override
    public String name() {
        return "tasks export";
    }

    @Override
    public String permission() {
        return "cee.admin";
    }

    @Override
    public boolean playerOnly() {
        return false;
    }

    @Override
    public List<ArgumentDefinition> arguments() {
        return List.of();
    }

    @Override
    public String description() {
        return "Exporta un catálogo YAML de tasks a disco.";
    }

    @Override
    public void execute(CommandContext context) {
        File folder = services.plugin().getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            messages.send(context.sender(), "tasks.export.error", Map.of("message", "No se pudo crear la carpeta del plugin."));
            return;
        }

        File file = new File(folder, "task-catalog.yml");
        try {
            YamlConfiguration catalog = buildCatalog();
            catalog.save(file);
            messages.send(context.sender(), "tasks.export.success", Map.of("file", file.getName()));
        } catch (Exception ex) {
            messages.send(context.sender(), "tasks.export.error", Map.of("message", ex.getMessage() == null ? "" : ex.getMessage()));
        }
    }

    private YamlConfiguration buildCatalog() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("generated_at", Instant.now().toString());

        Map<String, Object> global = new LinkedHashMap<>();
        for (TaskDefinition task : services.engine().getGlobalTasks().values()) {
            global.put(task.getName(), serializeTask(task));
        }
        config.set("tasks.global", global);

        Map<String, Object> events = new LinkedHashMap<>();
        services.engine().getDefinitions().forEach((eventId, definition) -> {
            if (definition.getTasks().isEmpty()) {
                return;
            }
            Map<String, Object> eventTasks = new LinkedHashMap<>();
            for (TaskDefinition task : definition.getTasks().values()) {
                eventTasks.put(task.getName(), serializeTask(task));
            }
            events.put(eventId, eventTasks);
        });
        config.set("tasks.events", events);

        return config;
    }

    private Map<String, Object> serializeTask(TaskDefinition task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("source", task.getSource());
        map.put("description", task.getDescription());

        Map<String, Object> params = new LinkedHashMap<>();
        for (TaskParameterDefinition param : task.getParameters().values()) {
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("type", param.getType().name().toLowerCase());
            def.put("required", param.isRequired());
            if (param.getDefaultValue() != null) {
                def.put("default", param.getDefaultValue());
            }
            params.put(param.getName(), def);
        }
        map.put("params", params);

        Map<String, Object> returns = new LinkedHashMap<>();
        for (TaskReturnDefinition ret : task.getReturns().values()) {
            returns.put(ret.getName(), Map.of("type", ret.getType().name().toLowerCase()));
        }
        map.put("returns", returns);

        map.put("example", Map.of(
            "task", Map.of(
                "name", task.getName(),
                "with", exampleWith(task),
                "into", exampleInto(task)
            )
        ));

        return map;
    }

    private Map<String, Object> exampleWith(TaskDefinition task) {
        Map<String, Object> with = new LinkedHashMap<>();
        for (TaskParameterDefinition param : task.getParameters().values()) {
            if (param.getDefaultValue() != null) {
                continue;
            }
            with.put(param.getName(), "<" + param.getType().name().toLowerCase() + ">");
        }
        return with;
    }

    private Map<String, Object> exampleInto(TaskDefinition task) {
        Map<String, Object> into = new LinkedHashMap<>();
        for (TaskReturnDefinition ret : task.getReturns().values()) {
            into.put(ret.getName(), ret.getName());
        }
        return into;
    }
}

