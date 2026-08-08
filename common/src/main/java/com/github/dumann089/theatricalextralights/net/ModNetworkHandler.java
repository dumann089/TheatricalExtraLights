package com.github.dumann089.theatricalextralights.net;

import dev.architectury.networking.NetworkChannel;
import net.minecraft.resources.ResourceLocation;

public class ModNetworkHandler {

    public static final NetworkChannel CHANNEL = NetworkChannel.create(
            new ResourceLocation("theatricalextralights", "main")
    );

    public static void register() {
        CHANNEL.register(SetJetHeightPacket.class, SetJetHeightPacket::encode, SetJetHeightPacket::decode, SetJetHeightPacket::handle);
        CHANNEL.register(SetJetThicknessPacket.class, SetJetThicknessPacket::encode, SetJetThicknessPacket::decode, SetJetThicknessPacket::handle);
        CHANNEL.register(SetJetConeAnglePacket.class, SetJetConeAnglePacket::encode, SetJetConeAnglePacket::decode, SetJetConeAnglePacket::handle);
        CHANNEL.register(SetFixturePositionPacket.class, SetFixturePositionPacket::encode, SetFixturePositionPacket::decode, SetFixturePositionPacket::handle);
        CHANNEL.register(SetPersonalityPacket.class, SetPersonalityPacket::encode, SetPersonalityPacket::decode, SetPersonalityPacket::handle);
        CHANNEL.register(FollowspotConsolePatchPacket.class, FollowspotConsolePatchPacket::encode, FollowspotConsolePatchPacket::decode, FollowspotConsolePatchPacket::handle);
        CHANNEL.register(FollowspotConsoleControlPacket.class, FollowspotConsoleControlPacket::encode, FollowspotConsoleControlPacket::decode, FollowspotConsoleControlPacket::handle);
        CHANNEL.register(FollowspotEnterControlPacket.class, FollowspotEnterControlPacket::encode, FollowspotEnterControlPacket::decode, FollowspotEnterControlPacket::handle);
        CHANNEL.register(FollowspotExitControlPacket.class, FollowspotExitControlPacket::encode, FollowspotExitControlPacket::decode, FollowspotExitControlPacket::handle);
        CHANNEL.register(ConfettiBurstPacket.class, ConfettiBurstPacket::encode, ConfettiBurstPacket::decode, ConfettiBurstPacket::handle);
        CHANNEL.register(SetLedFacadeConfigPacket.class, SetLedFacadeConfigPacket::encode, SetLedFacadeConfigPacket::decode, SetLedFacadeConfigPacket::handle);
        CHANNEL.register(SetLedFacadePixelsPacket.class, SetLedFacadePixelsPacket::encode, SetLedFacadePixelsPacket::decode, SetLedFacadePixelsPacket::handle);
    }
}
