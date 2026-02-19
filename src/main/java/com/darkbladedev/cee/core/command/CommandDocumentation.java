package com.darkbladedev.cee.core.command;

import java.util.List;

public final class CommandDocumentation {
    private CommandDocumentation() {
    }

    public static List<CommandInfo> defaultCommands() {
        return List.of(
            new CommandInfo(
                "help",
                "/cee help [comando]",
                "Muestra la ayuda general o detallada de un comando.",
                "cee.help",
                List.of("?"),
                List.of("/cee help", "/cee help start", "/cee ? list")
            ),
            new CommandInfo(
                "list",
                "/cee list [pagina] | /cee list all",
                "Lista eventos cargados con paginación o lista completa.",
                "cee.view",
                List.of("ls"),
                List.of("/cee list", "/cee list 2", "/cee list all")
            ),
            new CommandInfo(
                "reload",
                "/cee reload | /cee reload silent",
                "Recarga definiciones de eventos y triggers.",
                "cee.admin",
                List.of("rl"),
                List.of("/cee reload", "/cee reload silent")
            ),
            new CommandInfo(
                "start",
                "/cee event start <evento> [ubicacion]",
                "Inicia un evento en el chunk objetivo.",
                "cee.admin",
                List.of("run"),
                List.of("/cee event start chaos_storm", "/cee event start chaos_storm 100 64 100")
            ),
            new CommandInfo(
                "stop",
                "/cee event stop [ubicacion]",
                "Detiene el evento activo en el chunk objetivo.",
                "cee.admin",
                List.of("end"),
                List.of("/cee event stop", "/cee event stop 100 64 100")
            ),
            new CommandInfo(
                "status",
                "/cee event status [ubicacion]",
                "Muestra estado del evento activo en el chunk objetivo.",
                "cee.view",
                List.of("state"),
                List.of("/cee event status", "/cee event status 100 64 100")
            ),
            new CommandInfo(
                "inspect",
                "/cee event inspect [ubicacion]",
                "Muestra detalles del runtime activo en el chunk objetivo.",
                "cee.admin",
                List.of("info"),
                List.of("/cee event inspect", "/cee event inspect 100 64 100")
            ),
            new CommandInfo(
                "player",
                "/cee player info <jugador>",
                "Muestra el evento activo en el chunk del jugador.",
                "cee.view",
                List.of("p"),
                List.of("/cee player info DarkBladeDev")
            )
        );
    }
}
