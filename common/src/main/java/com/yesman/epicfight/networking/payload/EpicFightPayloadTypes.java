package com.yesman.epicfight.networking.payload;

import com.yesman.akythera.core.networking.payload.PayloadChannel;

import java.util.ArrayList;
import java.util.List;

/// Manager class for payload types pack a packet info as (type, codec, handler)
///
/// Called on each platform to register payload channel
public final class EpicFightPayloadTypes {
    public static final List<PayloadChannel<?>> CLIENT_BOUND = new ArrayList<>();
    public static final List<PayloadChannel<?>> SERVER_BOUND = new ArrayList<>();
    public static final List<PayloadChannel<?>> BI_DIRECTIONAL = new ArrayList<>();

    static {
        // client bound packets

        // server bound packets
    }
}
