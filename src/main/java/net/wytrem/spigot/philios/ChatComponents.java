package net.wytrem.spigot.philios;

import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.stream.Collector;
import java.util.stream.Collectors;

public class ChatComponents {
    public static Collector<BaseComponent, ?, BaseComponent> joining(String delimiter) {
        return joining(new TextComponent(delimiter));
    }
    public static Collector<BaseComponent, ?, BaseComponent> joining(BaseComponent delimiter) {
        return Collectors.reducing(new TextComponent(""), (a, b) -> {
            a.addExtra(delimiter);
            a.addExtra(b);
            return a;
        });
    }

    public static Collector<BaseComponent, ?, BaseComponent> joining() {
        return Collectors.reducing(new TextComponent(""), (a, b) -> {
            a.addExtra(b); return a;
        });
    }
}
