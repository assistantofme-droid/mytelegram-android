/*
 * Custom helper to route the Telegram client to a private MyTelegram backend.
 *
 * The official Telegram for Android client hard-codes the IP addresses of
 * Telegram's datacenters inside the native C++ libtmessages.dylib/libtmessages.so.
 * To make the client talk to a private backend at runtime, we override the
 * datacenter addresses with `ConnectionsManager.applyDatacenterAddress(...)`
 * as soon as the user account's ConnectionsManager is initialized.
 *
 * The override must happen BEFORE the first MTProto handshake. The best place
 * to do this is right after MessagesController is created for the account —
 * which is what ApplicationLoader does in its onCreate loop.
 *
 * Configuration:
 *   BuildVars.BACKEND_HOST — your server's IP (default 5.42.217.167)
 *   BuildVars.BACKEND_PORT — main DC port (default 20443)
 *
 * Port mapping for media DCs is derived from the main port by adding
 *   +1024, +2048, +3072, +4096 (the same offset scheme used by the
 *   mytelegram-dev docker-compose defaults).
 */
package org.telegram.messenger;

import org.telegram.tgnet.ConnectionsManager;

public final class BackendConfig {

    private static volatile boolean sInitialized = false;

    private BackendConfig() {}

    /**
     * Override all known datacenter addresses to point to the private backend.
     * Safe to call multiple times — only applies the override once per account.
     */
    public static void applyIfPending(int currentAccount) {
        if (sInitialized) {
            return;
        }
        // Apply for all 5 known DC ids (Telegram uses DC ids 1..5).
        // The native layer doesn't care which id we pass — applyDatacenterAddress
        // just rewrites the host:port for that id in the in-memory DC table.
        applyDc(currentAccount, 1, BuildVars.BACKEND_HOST, BuildVars.BACKEND_PORT);
        applyDc(currentAccount, 2, BuildVars.BACKEND_HOST, BuildVars.BACKEND_PORT + 1024);
        applyDc(currentAccount, 3, BuildVars.BACKEND_HOST, BuildVars.BACKEND_PORT + 2048);
        applyDc(currentAccount, 4, BuildVars.BACKEND_HOST, BuildVars.BACKEND_PORT + 3072);
        applyDc(currentAccount, 5, BuildVars.BACKEND_HOST, BuildVars.BACKEND_PORT + 4096);

        // Mark initialized on this account so we don't re-apply on every reconnect.
        sInitialized = true;
        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("BackendConfig: routed all DCs to " + BuildVars.BACKEND_HOST + ":" + BuildVars.BACKEND_PORT);
        }
    }

    private static void applyDc(int currentAccount, int dcId, String host, int port) {
        try {
            ConnectionsManager.getInstance(currentAccount).applyDatacenterAddress(dcId, host, port);
        } catch (Throwable t) {
            FileLog.e(t);
        }
    }
}
