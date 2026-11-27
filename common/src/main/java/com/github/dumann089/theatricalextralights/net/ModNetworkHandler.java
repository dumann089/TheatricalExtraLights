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
        CHANNEL.register(
                SetGoboFieldAnglePacket.class,
                SetGoboFieldAnglePacket::encode,
                SetGoboFieldAnglePacket::decode,
                SetGoboFieldAnglePacket::handle
        );
        CHANNEL.register(SetPersonalityPacket.class, SetPersonalityPacket::encode, SetPersonalityPacket::decode, SetPersonalityPacket::handle);
        CHANNEL.register(SetLaserSafetyPacket.class, SetLaserSafetyPacket::encode, SetLaserSafetyPacket::decode, SetLaserSafetyPacket::handle);
        CHANNEL.register(SetFixtureArmedPacket.class, SetFixtureArmedPacket::encode, SetFixtureArmedPacket::decode, SetFixtureArmedPacket::handle);
        CHANNEL.register(SetFlameStylePacket.class, SetFlameStylePacket::encode, SetFlameStylePacket::decode, SetFlameStylePacket::handle);
        CHANNEL.register(FollowspotPresetPacket.class, FollowspotPresetPacket::encode, FollowspotPresetPacket::decode, FollowspotPresetPacket::handle);
        CHANNEL.register(FollowspotConsolePatchPacket.class, FollowspotConsolePatchPacket::encode, FollowspotConsolePatchPacket::decode, FollowspotConsolePatchPacket::handle);
        CHANNEL.register(FollowspotConsoleControlPacket.class, FollowspotConsoleControlPacket::encode, FollowspotConsoleControlPacket::decode, FollowspotConsoleControlPacket::handle);
        CHANNEL.register(FollowspotEnterControlPacket.class, FollowspotEnterControlPacket::encode, FollowspotEnterControlPacket::decode, FollowspotEnterControlPacket::handle);
        CHANNEL.register(FollowspotExitControlPacket.class, FollowspotExitControlPacket::encode, FollowspotExitControlPacket::decode, FollowspotExitControlPacket::handle);
        CHANNEL.register(ConfettiBurstPacket.class, ConfettiBurstPacket::encode, ConfettiBurstPacket::decode, ConfettiBurstPacket::handle);
        CHANNEL.register(SetMountTransformPacket.class, SetMountTransformPacket::encode, SetMountTransformPacket::decode, SetMountTransformPacket::handle);
        CHANNEL.register(SetLedFacadeConfigPacket.class, SetLedFacadeConfigPacket::encode, SetLedFacadeConfigPacket::decode, SetLedFacadeConfigPacket::handle);
        CHANNEL.register(SetLedFacadePixelsPacket.class, SetLedFacadePixelsPacket::encode, SetLedFacadePixelsPacket::decode, SetLedFacadePixelsPacket::handle);
        CHANNEL.register(
                SetBigScrollerGelConfigPacket.class,
                SetBigScrollerGelConfigPacket::encode,
                SetBigScrollerGelConfigPacket::decode,
                SetBigScrollerGelConfigPacket::handle
        );
        CHANNEL.register(SetLaserProjectorSettingsPacket.class, SetLaserProjectorSettingsPacket::encode, SetLaserProjectorSettingsPacket::decode, SetLaserProjectorSettingsPacket::handle);

        // --- Cameras / broadcast (no fim: o ID do packet depende da ordem de registro) ---
        CHANNEL.register(BroadcastCameraAimPacket.class, BroadcastCameraAimPacket::encode, BroadcastCameraAimPacket::decode, BroadcastCameraAimPacket::handle);
        CHANNEL.register(BroadcastCameraExitPacket.class, BroadcastCameraExitPacket::encode, BroadcastCameraExitPacket::decode, BroadcastCameraExitPacket::handle);
        CHANNEL.register(BroadcastCameraHeightPacket.class, BroadcastCameraHeightPacket::encode, BroadcastCameraHeightPacket::decode, BroadcastCameraHeightPacket::handle);
        CHANNEL.register(JibCameraBoomPacket.class, JibCameraBoomPacket::encode, JibCameraBoomPacket::decode, JibCameraBoomPacket::handle);
        CHANNEL.register(SyncRequestCamerasPacket.class, SyncRequestCamerasPacket::encode, SyncRequestCamerasPacket::decode, SyncRequestCamerasPacket::handle);
        CHANNEL.register(SyncCameraListPacket.class, SyncCameraListPacket::encode, SyncCameraListPacket::decode, SyncCameraListPacket::handle);
        CHANNEL.register(VideoSwitcherConfigPacket.class, VideoSwitcherConfigPacket::encode, VideoSwitcherConfigPacket::decode, VideoSwitcherConfigPacket::handle);
        CHANNEL.register(VideoSwitcherProgramPacket.class, VideoSwitcherProgramPacket::encode, VideoSwitcherProgramPacket::decode, VideoSwitcherProgramPacket::handle);
        CHANNEL.register(VideoSwitcherSequencePacket.class, VideoSwitcherSequencePacket::encode, VideoSwitcherSequencePacket::decode, VideoSwitcherSequencePacket::handle);
        CHANNEL.register(JumbotronConfigPacket.class, JumbotronConfigPacket::encode, JumbotronConfigPacket::decode, JumbotronConfigPacket::handle);
        CHANNEL.register(BroadcastCameraSettingsPacket.class, BroadcastCameraSettingsPacket::encode, BroadcastCameraSettingsPacket::decode, BroadcastCameraSettingsPacket::handle);
        CHANNEL.register(CameraSyncControlPacket.class, CameraSyncControlPacket::encode, CameraSyncControlPacket::decode, CameraSyncControlPacket::handle);
        CHANNEL.register(CameraPlaybackPacket.class, CameraPlaybackPacket::encode, CameraPlaybackPacket::decode, CameraPlaybackPacket::handle);
    }
}
