package com.epicstreaming.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;

/** API 21+ media session and lock-screen metadata. Loaded only on Lollipop and newer. */
final class MediaSessionBridge {
    interface Controls { void toggle(); }

    private final Context context;
    private final MediaSession session;
    private final Controls controls;
    private String title = "Epic Streaming";
    private String artist = "";
    private String album = "";
    private Bitmap art;
    private boolean playing;

    MediaSessionBridge(Context context, Controls controls) {
        this.context = context;
        this.controls = controls;
        session = new MediaSession(context, "Epic Streaming");
        session.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS | MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { MediaSessionBridge.this.controls.toggle(); }
            @Override public void onPause() { MediaSessionBridge.this.controls.toggle(); }
            @Override public void onStop() { MediaSessionBridge.this.controls.toggle(); }
        });
        session.setActive(true);
    }

    void update(String title, String artist, String album, Bitmap art, boolean playing) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        if (art != null) this.art = art;
        this.playing = playing;
        MediaMetadata.Builder metadata = new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album);
        if (this.art != null) {
            metadata.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, this.art);
            metadata.putBitmap(MediaMetadata.METADATA_KEY_ART, this.art);
        }
        session.setMetadata(metadata.build());
        PlaybackState state = new PlaybackState.Builder()
                .setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP)
                .setState(playing ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build();
        session.setPlaybackState(state);
    }

    Notification notification(PendingIntent toggleIntent) {
        Notification.Action action = new Notification.Action.Builder(
                playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                playing ? "Pause" : "Play", toggleIntent).build();
        Notification.Builder builder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle(title)
                .setContentText(artist + (album.length() == 0 ? "" : " • " + album))
                .setLargeIcon(art)
                .setContentIntent(PendingIntent.getActivity(context, 0,
                        new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT))
                .setShowWhen(false)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0))
                .addAction(action);
        if (Build.VERSION.SDK_INT >= 26) builder.setChannelId("epic_music_playback");
        return builder.build();
    }

    void release() {
        session.setActive(false);
        session.release();
    }
}
