package net.momirealms.craftengine.bukkit.plugin.command.feature;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.command.BukkitCommandFeature;
import net.momirealms.craftengine.bukkit.plugin.web.ContentWebServer;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.command.CraftEngineCommandManager;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.parser.standard.StringParser;
import java.io.IOException;
import java.util.Locale;

public final class WebControlCommand extends BukkitCommandFeature<CommandSender> {
    public WebControlCommand(CraftEngineCommandManager<CommandSender> manager, CraftEngine plugin) {
        super(manager, plugin);
    }
    @Override public String getFeatureID() { return "web_control"; }
    @Override public Command.Builder<? extends CommandSender> assembleCommand(
            org.incendo.cloud.CommandManager<CommandSender> manager,
            Command.Builder<CommandSender> builder) {
        return builder.optional("action", StringParser.stringParser()).handler(context -> {
            ContentWebServer web = ((BukkitCraftEngine) plugin()).contentWebServer();
            if (web == null) {
                context.sender().sendMessage("VSRender: la web no esta inicializada.");
                return;
            }
            String action = context.getOrDefault("action", "status").toLowerCase(Locale.ROOT);
            switch (action) {
                case "start", "on", "encender" -> {
                    try {
                        context.sender().sendMessage("VSRender web: " + web.start());
                        context.sender().sendMessage("Solo accesible en localhost. No compartas el enlace.");
                    } catch (IOException error) {
                        context.sender().sendMessage("VSRender web: no se pudo usar el puerto 3050. Cerra la web Python anterior.");
                        plugin().logger().warn("Could not start local VSRender web editor", error);
                    }
                }
                case "stop", "off", "apagar" -> {
                    web.stop();
                    context.sender().sendMessage("VSRender web: apagada.");
                }
                case "status", "estado" -> context.sender().sendMessage(
                        web.running() ? "VSRender web: " + web.link()
                                : "VSRender web: apagada. Usa /vsr web start.");
                default -> context.sender().sendMessage("Uso: /vsr web start | stop | status");
            }
        });
    }
}
