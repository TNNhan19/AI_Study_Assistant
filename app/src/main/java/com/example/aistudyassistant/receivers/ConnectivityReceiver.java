package com.example.aistudyassistant.receivers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

import androidx.annotation.NonNull;

import com.example.aistudyassistant.utils.Constants;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CopyOnWriteArraySet;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** Theo dõi kết nối Internet đã được Android xác thực. */
public final class ConnectivityReceiver {

    public interface Listener {
        void onNetworkAvailable();
    }

    private static ConnectivityReceiver instance;

    private final ConnectivityManager connectivityManager;
    private final Set<Listener> listeners = new CopyOnWriteArraySet<>();
    private final ExecutorService probeExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean probeRunning = new AtomicBoolean(false);
    private final OkHttpClient probeClient = new OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build();
    private volatile boolean connected;
    private volatile boolean transportFailureReported;

    private final ConnectivityManager.NetworkCallback networkCallback =
            new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    refreshConnectionState();
                }

                @Override
                public void onCapabilitiesChanged(@NonNull Network network,
                                                  @NonNull NetworkCapabilities capabilities) {
                    refreshConnectionState();
                }

                @Override
                public void onLost(@NonNull Network network) {
                    refreshConnectionState();
                }
            };

    private ConnectivityReceiver(Context context) {
        connectivityManager = (ConnectivityManager) context.getApplicationContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        connected = hasValidatedInternet();
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    public static synchronized ConnectivityReceiver getInstance(Context context) {
        if (instance == null) {
            instance = new ConnectivityReceiver(context);
        }
        return instance;
    }

    public boolean isConnected() {
        return connected;
    }

    public void addListener(Listener listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        if (listener != null) listeners.remove(listener);
    }

    public void reportTransportFailure() {
        transportFailureReported = true;
        updateConnectionState(false);
        startReachabilityProbe();
    }

    private void refreshConnectionState() {
        if (transportFailureReported) {
            startReachabilityProbe();
            return;
        }
        updateConnectionState(hasValidatedInternet());
    }

    private void startReachabilityProbe() {
        if (!probeRunning.compareAndSet(false, true)) return;
        probeExecutor.execute(() -> {
            try {
                while (transportFailureReported) {
                    if (hasValidatedInternet() && canReachSupabase()) {
                        transportFailureReported = false;
                        updateConnectionState(true);
                        return;
                    }
                    Thread.sleep(3_000);
                }
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            } finally {
                probeRunning.set(false);
                if (transportFailureReported) startReachabilityProbe();
            }
        });
    }

    private boolean canReachSupabase() {
        Request request = new Request.Builder()
                .url(Constants.SUPABASE_URL + "/auth/v1/health")
                .get()
                .build();
        try (Response ignored = probeClient.newCall(request).execute()) {
            // Nhận được bất kỳ HTTP response nào nghĩa là đường truyền đã hoạt động.
            return true;
        } catch (IOException error) {
            return false;
        }
    }

    private void updateConnectionState(boolean nowConnected) {
        boolean becameAvailable;
        synchronized (this) {
            becameAvailable = !connected && nowConnected;
            connected = nowConnected;
        }
        if (!becameAvailable) return;
        for (Listener listener : listeners) {
            listener.onNetworkAvailable();
        }
    }

    private boolean hasValidatedInternet() {
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;

        NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
