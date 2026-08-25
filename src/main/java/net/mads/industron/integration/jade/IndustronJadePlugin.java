package net.mads.industron.integration.jade;

import net.mads.industron.Industron;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin(Industron.MOD_ID)
public final class IndustronJadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
    }
}