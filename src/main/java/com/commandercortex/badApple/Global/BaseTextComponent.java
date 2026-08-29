package com.commandercortex.badApple.Global;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;

/**
 * Small helper to create clickable/hoverable base TextComponent instances for chat components.
 */
public class BaseTextComponent {
    public static TextComponent LinkBaseText(String message, String clickEvent, String hoverEvent){
        TextComponent component = new TextComponent(message);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, clickEvent));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverEvent)));
        return component;
    }
}
