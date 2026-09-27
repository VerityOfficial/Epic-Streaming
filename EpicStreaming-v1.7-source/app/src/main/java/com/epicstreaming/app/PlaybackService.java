package com.epicstreaming.app;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Random;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Keeps audio, notification controls, and media metadata alive when the screen is off. */
public class PlaybackService extends Service {
    public static final String ACTION_PLAY = "com.epicstreaming.app.PLAY";
    public static final String ACTION_TOGGLE = "com.epicstreaming.app.TOGGLE";
    public static final String ACTION_PREVIOUS = "com.epicstreaming.app.PREVIOUS";
    public static final String ACTION_NEXT = "com.epicstreaming.app.NEXT";
    public static final String ACTION_SEEK_BACK = "com.epicstreaming.app.SEEK_BACK";
    public static final String ACTION_SEEK_FORWARD = "com.epicstreaming.app.SEEK_FORWARD";
    public static final String ACTION_SEEK_TO = "com.epicstreaming.app.SEEK_TO";
    public static final String ACTION_STATE_QUERY = "com.epicstreaming.app.STATE_QUERY";
    public static final String ACTION_STATE = "com.epicstreaming.app.STATE";
    public static final String EXTRA_URL = "streamUrl";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_ARTIST = "artist";
    public static final String EXTRA_ALBUM = "album";
    public static final String EXTRA_COVER = "coverUrl";
    public static final String EXTRA_COVER_ID = "coverArtId";
    public static final String EXTRA_TRACK_ID = "trackId";
    public static final String EXTRA_SIMILAR_TEMPLATE = "similarTemplate";
    public static final String EXTRA_ARTIST_SEARCH_TEMPLATE = "artistSearchTemplate";
    public static final String EXTRA_STREAM_TEMPLATE = "streamTemplate";
    public static final String EXTRA_COVER_TEMPLATE = "coverTemplate";
    public static final String EXTRA_RANDOM_TEMPLATE = "randomTemplate";
    public static final String EXTRA_POSITION = "position";
    public static final String EXTRA_DURATION_MS = "durationMs";
    private static final int NOTIFICATION_ID = 412;
    private static final String CHANNEL_ID = "epic_music_playback";

    private MediaPlayer player;
    private MediaSessionBridge mediaSession;
    private String title = "Epic Streaming", artist = "", album = "", coverUrl = "", coverArtId = "";
    private String trackId = "", similarTemplate = "", artistSearchTemplate = "", streamTemplate = "", coverTemplate = "";
    private String randomTemplate = "", stateText = "Nothing playing";
    private int trackDurationMs;
    private Bitmap cover;
    private boolean prepared;
    private int playbackGeneration;
    private final ArrayList<Track> history = new ArrayList<>();
    private final Handler progressHandler = new Handler();
    private final Runnable progressUpdater = new Runnable() {
        @Override public void run() {
            if (prepared && player != null) {
                publishState(stateText);
                progressHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 21) mediaSession = new MediaSessionBridge(this, this::toggle);
        createChannel();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_TOGGLE.equals(action)) {
            toggle();
        } else if (ACTION_PREVIOUS.equals(action)) {
            playPrevious();
        } else if (ACTION_NEXT.equals(action)) {
            playRandomNext();
        } else if (ACTION_SEEK_BACK.equals(action)) {
            seekBy(-10000);
        } else if (ACTION_SEEK_FORWARD.equals(action)) {
            seekBy(10000);
        } else if (ACTION_SEEK_TO.equals(action)) {
            seekTo(intent.getIntExtra(EXTRA_POSITION, 0));
        } else if (ACTION_STATE_QUERY.equals(action)) {
            if (player != null) publishState(stateText);
            else stopSelf(startId);
        } else if (ACTION_PLAY.equals(action)) {
            startTrack(intent);
        }
        return START_NOT_STICKY;
    }

    private void startTrack(Intent intent) { startTrack(intent, true); }

    private void startTrack(Intent intent, boolean rememberCurrent) {
        playbackGeneration++;
        String nextTrackId = value(intent.getStringExtra(EXTRA_TRACK_ID));
        if (rememberCurrent && trackId.length() > 0 && !trackId.equals(nextTrackId)) {
            Track oldTrack = new Track();
            oldTrack.id = trackId;
            oldTrack.title = title;
            oldTrack.artist = artist;
            oldTrack.album = album;
            oldTrack.cover = coverArtId;
            oldTrack.durationMs = trackDurationMs;
            history.add(oldTrack);
            if (history.size() > 100) history.remove(0);
        }
        title = intent.getStringExtra(EXTRA_TITLE);
        artist = intent.getStringExtra(EXTRA_ARTIST);
        album = intent.getStringExtra(EXTRA_ALBUM);
        coverUrl = intent.getStringExtra(EXTRA_COVER);
        coverArtId = value(intent.getStringExtra(EXTRA_COVER_ID));
        trackDurationMs = intent.getIntExtra(EXTRA_DURATION_MS, 0);
        trackId = nextTrackId;
        similarTemplate = value(intent.getStringExtra(EXTRA_SIMILAR_TEMPLATE));
        artistSearchTemplate = value(intent.getStringExtra(EXTRA_ARTIST_SEARCH_TEMPLATE));
        streamTemplate = value(intent.getStringExtra(EXTRA_STREAM_TEMPLATE));
        coverTemplate = value(intent.getStringExtra(EXTRA_COVER_TEMPLATE));
        randomTemplate = value(intent.getStringExtra(EXTRA_RANDOM_TEMPLATE));
        if (title == null) title = "Unknown track";
        if (artist == null) artist = "";
        if (album == null) album = "";
        if (coverUrl == null) coverUrl = "";
        cover = null;
        prepared = false;
        progressHandler.removeCallbacks(progressUpdater);
        Intent toggle = new Intent(this, PlaybackService.class).setAction(ACTION_TOGGLE);
        PendingIntent toggleIntent = PendingIntent.getService(this, 1, toggle, PendingIntent.FLAG_UPDATE_CURRENT);
        startForeground(NOTIFICATION_ID, buildNotification(toggleIntent, false));
        stateText = "Loading: " + title;
        publishState(stateText);
        if (player != null) { player.release(); player = null; }
        player = new MediaPlayer();
        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
        player.setOnPreparedListener(mp -> {
            prepared = true;
            mp.start();
            updateMedia(true);
            refreshNotification(true);
            stateText = "Playing: " + title;
            publishState(stateText);
            progressHandler.removeCallbacks(progressUpdater);
            progressHandler.postDelayed(progressUpdater, 1000);
        });
        player.setOnCompletionListener(mp -> {
            prepared = false;
            progressHandler.removeCallbacks(progressUpdater);
            updateMedia(false);
            refreshNotification(false);
            findSimilarTrack();
        });
        player.setOnErrorListener((mp, what, extra) -> {
            prepared = false;
            progressHandler.removeCallbacks(progressUpdater);
            updateMedia(false);
            refreshNotification(false);
            publishState("Can't play " + title + " (player error " + what + ", " + extra + ")");
            return true;
        });
        try {
            String streamUrl = intent.getStringExtra(EXTRA_URL);
            if (streamUrl == null || streamUrl.length() == 0) throw new IllegalArgumentException("Missing stream URL");
            player.setDataSource(this, android.net.Uri.parse(streamUrl));
            player.prepareAsync();
        } catch (Exception e) {
            publishState("Could not start playback: " + e.getMessage());
            stopForeground(true);
            stopSelf();
            return;
        }
        loadCover();
    }

    private String value(String value) { return value == null ? "" : value; }

    private void findSimilarTrack() {
        if (trackId.length() == 0 || streamTemplate.length() == 0) {
            publishState("Finished: " + title);
            return;
        }
        final int requestedGeneration = ++playbackGeneration;
        final String finishedTitle = title;
        publishState("Finding similar music after " + finishedTitle);
        new AsyncTask<Void, Void, Track>() {
            @Override protected Track doInBackground(Void... params) {
                ArrayList<Track> choices = new ArrayList<>();
                if (similarTemplate.length() > 0) {
                    choices = fetchTracks(similarTemplate + "&id=" + encode(trackId), trackId);
                }
                if (choices.size() == 0 && artist.length() > 0 && artistSearchTemplate.length() > 0) {
                    choices = fetchTracks(artistSearchTemplate + "&query=" + encode(artist), trackId);
                }
                if (choices.size() == 0) return null;
                return choices.get(new Random().nextInt(choices.size()));
            }

            @Override protected void onPostExecute(Track next) {
                if (requestedGeneration != playbackGeneration) return;
                if (next == null) {
                    publishState("Finished: " + finishedTitle + " · no similar tracks found");
                    return;
                }
                Intent nextTrack = new Intent(PlaybackService.this, PlaybackService.class)
                        .setAction(ACTION_PLAY)
                        .putExtra(EXTRA_URL, streamTemplate + "&id=" + encode(next.id))
                        .putExtra(EXTRA_TRACK_ID, next.id)
                        .putExtra(EXTRA_TITLE, next.title)
                        .putExtra(EXTRA_ARTIST, next.artist)
                        .putExtra(EXTRA_ALBUM, next.album)
                        .putExtra(EXTRA_COVER_ID, next.cover)
                        .putExtra(EXTRA_DURATION_MS, next.durationMs)
                        .putExtra(EXTRA_SIMILAR_TEMPLATE, similarTemplate)
                        .putExtra(EXTRA_ARTIST_SEARCH_TEMPLATE, artistSearchTemplate)
                        .putExtra(EXTRA_STREAM_TEMPLATE, streamTemplate)
                        .putExtra(EXTRA_COVER_TEMPLATE, coverTemplate)
                        .putExtra(EXTRA_RANDOM_TEMPLATE, randomTemplate);
                if (next.cover.length() > 0 && coverTemplate.length() > 0) {
                    nextTrack.putExtra(EXTRA_COVER, coverTemplate + "&id=" + encode(next.cover));
                }
                startTrack(nextTrack);
            }
        }.execute();
    }

    private ArrayList<Track> fetchTracks(String url, String excludeId) {
        ArrayList<Track> tracks = new ArrayList<>();
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("User-Agent", "EpicStreaming/1.0 Android");
            input = connection.getInputStream();
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
            Element response = document.getDocumentElement();
            if (response == null || !"ok".equals(response.getAttribute("status"))) return tracks;
            NodeList songs = document.getElementsByTagName("song");
            for (int i = 0; i < songs.getLength(); i++) {
                Element song = (Element) songs.item(i);
                String id = song.getAttribute("id");
                if (id.length() == 0 || id.equals(excludeId)) continue;
                Track track = new Track();
                track.id = id;
                track.title = attr(song, "title", attr(song, "name", "Unknown track"));
                track.artist = attr(song, "artist", artist);
                track.album = attr(song, "album", "");
                track.cover = attr(song, "coverArt", "");
                try { track.durationMs = (int) (Double.parseDouble(attr(song, "duration", "0")) * 1000); }
                catch (NumberFormatException ignored) { track.durationMs = 0; }
                tracks.add(track);
            }
        } catch (Exception ignored) {
            // A missing Last.fm integration is normal; try same-artist tracks next.
        } finally {
            try { if (input != null) input.close(); } catch (Exception ignored) { }
            if (connection != null) connection.disconnect();
        }
        return tracks;
    }

    private String attr(Element element, String name, String fallback) {
        String value = element.getAttribute(name);
        return value.length() == 0 ? fallback : value;
    }

    private String encode(String value) {
        try { return URLEncoder.encode(value, "UTF-8"); }
        catch (Exception ignored) { return value; }
    }

    private static final class Track {
        String id, title, artist, album, cover;
        int durationMs;
    }

    private void toggle() {
        if (player == null || !prepared) return;
        try {
            if (player.isPlaying()) {
                player.pause();
                updateMedia(false);
                refreshNotification(false);
                stateText = "Paused: " + title;
                publishState(stateText);
            } else {
                player.start();
                updateMedia(true);
                refreshNotification(true);
                stateText = "Playing: " + title;
                publishState(stateText);
                progressHandler.removeCallbacks(progressUpdater);
                progressHandler.postDelayed(progressUpdater, 1000);
            }
        } catch (IllegalStateException ignored) { }
    }

    private void seekBy(int amount) {
        if (player == null || !prepared) return;
        try { seekTo(player.getCurrentPosition() + amount); }
        catch (IllegalStateException ignored) { }
    }

    private void seekTo(int position) {
        if (player == null || !prepared) return;
        try {
            int duration = player.getDuration();
            if (duration <= 0) duration = trackDurationMs;
            player.seekTo(Math.max(0, Math.min(position, duration)));
            publishState(stateText);
        } catch (IllegalStateException ignored) { }
    }

    private void playPrevious() {
        if (history.size() == 0) {
            stateText = "No previous track";
            publishState(stateText);
            return;
        }
        Track previous = history.remove(history.size() - 1);
        startTrack(trackIntent(previous), false);
    }

    private void playRandomNext() {
        if (randomTemplate.length() == 0) {
            stateText = "Random next track is unavailable";
            publishState(stateText);
            return;
        }
        final int requestedGeneration = ++playbackGeneration;
        stateText = "Finding a random next track";
        publishState(stateText);
        new AsyncTask<Void, Void, Track>() {
            @Override protected Track doInBackground(Void... params) {
                ArrayList<Track> choices = fetchTracks(randomTemplate, trackId);
                if (choices.size() == 0) return null;
                return choices.get(new Random().nextInt(choices.size()));
            }
            @Override protected void onPostExecute(Track next) {
                if (requestedGeneration != playbackGeneration) return;
                if (next == null) {
                    stateText = "Could not find a next track";
                    publishState(stateText);
                    return;
                }
                startTrack(trackIntent(next), true);
            }
        }.execute();
    }

    private Intent trackIntent(Track track) {
        Intent intent = new Intent(this, PlaybackService.class).setAction(ACTION_PLAY)
                .putExtra(EXTRA_URL, streamTemplate + "&id=" + encode(track.id))
                .putExtra(EXTRA_TRACK_ID, track.id)
                .putExtra(EXTRA_TITLE, track.title)
                .putExtra(EXTRA_ARTIST, track.artist)
                .putExtra(EXTRA_ALBUM, track.album)
                .putExtra(EXTRA_COVER_ID, track.cover)
                .putExtra(EXTRA_DURATION_MS, track.durationMs)
                .putExtra(EXTRA_SIMILAR_TEMPLATE, similarTemplate)
                .putExtra(EXTRA_ARTIST_SEARCH_TEMPLATE, artistSearchTemplate)
                .putExtra(EXTRA_STREAM_TEMPLATE, streamTemplate)
                .putExtra(EXTRA_COVER_TEMPLATE, coverTemplate)
                .putExtra(EXTRA_RANDOM_TEMPLATE, randomTemplate);
        if (track.cover.length() > 0 && coverTemplate.length() > 0) {
            intent.putExtra(EXTRA_COVER, coverTemplate + "&id=" + encode(track.cover));
        }
        return intent;
    }

    private void loadCover() {
        if (coverUrl.length() == 0) return;
        final int requestedGeneration = playbackGeneration;
        final String requestedUrl = coverUrl;
        new AsyncTask<Void, Void, Bitmap>() {
            @Override protected Bitmap doInBackground(Void... params) {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL(requestedUrl).openConnection();
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(15000);
                    connection.setRequestProperty("User-Agent", "EpicStreaming/1.0 Android");
                    InputStream input = connection.getInputStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(input);
                    input.close();
                    return bitmap;
                } catch (Exception ignored) { return null; }
                finally { if (connection != null) connection.disconnect(); }
            }
            @Override protected void onPostExecute(Bitmap bitmap) {
                if (bitmap == null || requestedGeneration != playbackGeneration) return;
                cover = bitmap;
                updateMedia(prepared && player != null && player.isPlaying());
                refreshNotification(prepared && player != null && player.isPlaying());
            }
        }.execute();
    }

    private void updateMedia(boolean playing) {
        if (mediaSession != null) mediaSession.update(title, artist, album, cover, playing);
    }

    private Notification buildNotification(PendingIntent toggleIntent, boolean playing) {
        if (Build.VERSION.SDK_INT >= 21 && mediaSession != null) {
            mediaSession.update(title, artist, album, cover, playing);
            return mediaSession.notification(toggleIntent);
        }
        PendingIntent open = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT);
        if (Build.VERSION.SDK_INT >= 16) {
            Notification.Builder builder = new Notification.Builder(this)
                    .setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(title)
                    .setContentText(artist + (album.length() == 0 ? "" : " • " + album))
                    .setContentIntent(open).setOngoing(playing).setLargeIcon(cover);
            if (Build.VERSION.SDK_INT >= 26) builder.setChannelId(CHANNEL_ID);
            builder.addAction(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play,
                    playing ? "Pause" : "Play", toggleIntent);
            return builder.build();
        }
        Notification notification = new Notification(android.R.drawable.ic_media_play, title, System.currentTimeMillis());
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        try {
            Notification.class.getMethod("setLatestEventInfo", android.content.Context.class, CharSequence.class, CharSequence.class, PendingIntent.class)
                    .invoke(notification, this, title, artist + (album.length() == 0 ? "" : " • " + album), open);
        } catch (Exception ignored) { }
        return notification;
    }

    private void refreshNotification(boolean playing) {
        PendingIntent toggle = PendingIntent.getService(this, 1,
                new Intent(this, PlaybackService.class).setAction(ACTION_TOGGLE), PendingIntent.FLAG_UPDATE_CURRENT);
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, buildNotification(toggle, playing));
    }

    private void publishState(String state) {
        int position = 0, duration = 0;
        boolean playing = false;
        try {
            if (player != null && prepared) {
                position = player.getCurrentPosition();
                duration = player.getDuration();
                if (duration <= 0) duration = trackDurationMs;
                playing = player.isPlaying();
            }
        } catch (IllegalStateException ignored) { }
        Intent intent = new Intent(ACTION_STATE).setPackage(getPackageName())
                .putExtra("state", state).putExtra("title", title).putExtra("artist", artist)
                .putExtra("album", album).putExtra("coverUrl", coverUrl)
                .putExtra("position", position).putExtra("duration", duration)
                .putExtra("playing", playing);
        sendBroadcast(intent);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        Api26Channel.create(this, CHANNEL_ID);
    }

    @Override public void onDestroy() {
        progressHandler.removeCallbacks(progressUpdater);
        if (player != null) { player.release(); player = null; }
        if (mediaSession != null) { mediaSession.release(); mediaSession = null; }
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private static final class Api26Channel {
        static void create(Service service, String id) {
            android.app.NotificationChannel channel = new android.app.NotificationChannel(id, "Music playback", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Now playing controls and song information");
            ((NotificationManager) service.getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }
}
