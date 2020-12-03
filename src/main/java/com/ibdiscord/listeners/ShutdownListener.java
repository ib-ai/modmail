package com.ibdiscord.listeners;

import com.ibdiscord.data.db.DataContainer;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class ShutdownListener extends ListenerAdapter {

    @Override
    public void onShutdown(ShutdownEvent event) {
        DataContainer.INSTANCE.close();
    }
}
