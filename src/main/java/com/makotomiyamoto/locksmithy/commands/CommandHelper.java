package com.makotomiyamoto.locksmithy.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class CommandHelper implements TabCompleter {

    String[] options = {"help", "break", "debug", "generate"};
    String[] generate_options = {"lockpick", "blank_key", "advanced_blank_key"};

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String label, String[] args) {

        List<String> list = new ArrayList<>();

        if (args.length == 1) {
            for (String l : options) {
                if (l.startsWith(args[0])) {
                    list.add(l);
                }
            }
            return list;
        } else if (args.length == 2 && args[0].equals(options[3])) {
            for (String l : generate_options) {
                if (l.startsWith(args[1])) {
                    list.add(l);
                }
            }
            return list;
        }

        return null;

    }

}
