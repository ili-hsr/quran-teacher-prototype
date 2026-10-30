package org.example.quranteacher;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;

/**
 * Lecteur des récitations authentiques (fichiers locaux uniquement).
 * Une seule piste à la fois ; libération systématique pour ne pas laisser
 * le décodeur ouvert sur un téléphone de 2 Go.
 */
public final class RecitationPlayer {

    public interface Listener {
        void onFinished();

        void onFailed();
    }

    private final Context context;
    private MediaPlayer player;

    public RecitationPlayer(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isPlaying() {
        try {
            return player != null && player.isPlaying();
        } catch (Exception e) {
            return false;
        }
    }

    public void play(Uri uri, Listener listener) {
        stop();
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            player.setDataSource(context, uri);
            player.setOnCompletionListener(mp -> {
                release();
                if (listener != null) {
                    listener.onFinished();
                }
            });
            player.setOnErrorListener((mp, what, extra) -> {
                release();
                if (listener != null) {
                    listener.onFailed();
                }
                return true;
            });
            player.prepare();
            player.start();
        } catch (Exception e) {
            release();
            if (listener != null) {
                listener.onFailed();
            }
        }
    }

    public void stop() {
        try {
            if (player != null && player.isPlaying()) {
                player.stop();
            }
        } catch (Exception ignored) {
        }
        release();
    }

    private void release() {
        if (player != null) {
            try {
                player.reset();
                player.release();
            } catch (Exception ignored) {
            }
            player = null;
        }
    }
}
