/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mass_hysteria.moviedream;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.service.dreams.DreamService;
import android.util.Log;
import android.widget.VideoView;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Calendar;

//import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
//import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

public class MovieDaydream extends DreamService implements MediaPlayer.OnErrorListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private static final String TAG = MovieDaydream.class.getSimpleName();

    private static final String ACTION_USB_PERMISSION = "org.mass_hysteria.moviedream.USB_PERMISSION";

    private int currentMovie;
    private File[] movies;
    private VideoView videoView;

    private void sendToTv(String message) {
        SendToTv sender = new SendToTv();
        sender.execute(message);
    }

    private class SendToTv extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String[] messages) {
            Socket sock = null;
            try {
                Log.d(TAG,"Starting up socket...");
                sock = new Socket("10.20.200.146", 10002);
                PrintWriter out = new PrintWriter(sock.getOutputStream(), true);
                out.print("tv\r");
                out.flush();
                Thread.sleep(1000);
                out.print("pass\r");
                out.flush();
                Thread.sleep(1000);
                Log.d(TAG,"Sending message: " + messages[0]);
                out.print(messages[0] + "\r");
                out.flush();
                Thread.sleep(1000);
                sock.close();
                Log.d(TAG,"Closing socket");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    private void tvPowerOn() {
        sendToTv("POWR1   ");
    }

    private void tvPowerOff() {
        sendToTv("POWR0   ");
    }

    private void tvSetInput(int input) {
        sendToTv("IAVD" + input + "   ");
    }

    public void refreshFiles() {
        //get list of movie files from a directory
        File folder = new File("/sdcard/dream");
        movies = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                if (filename.toLowerCase().endsWith(".mp4")) {
                    return true;
                }
                if (filename.toLowerCase().endsWith(".3gp")) {
                    return true;
                }
                return false;
            }
        });
    }

    public void onPrepared(MediaPlayer mp) {
        Log.d(TAG, "Hardware view? " + videoView.isHardwareAccelerated());
        mp.setVolume(0.0f,0.0f);
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        if (hour >= 6 && hour < 18) {
            tvPowerOn();
            tvSetInput(2);
        } else {
            tvPowerOff();
        }
        mp.start();
    }

    public boolean onError(MediaPlayer mp, int what, int code) {
        onCompletion(mp);
        return true;
    }

    public void onCompletion(MediaPlayer mp) {
        mp.reset();
        if (currentMovie >= movies.length-1) {
            currentMovie = 0;
            refreshFiles();
        } else {
            currentMovie++;
        }
        try {
            Log.d(TAG, "Current movie: " + movies[currentMovie].getCanonicalPath());
        } catch (Exception e) {
            Log.d(TAG, "Current movie (not found?): " + movies[currentMovie].getName());
        }
        videoView.setVideoURI(Uri.fromFile(movies[currentMovie]));
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        sendToTv("RSPW2   \r");

        // Exit dream upon user touch
        setInteractive(false);
        // Hide system UI
        setFullscreen(true);
        // Set the dream layout
        setContentView(R.layout.dream);

        refreshFiles();

        //start the first movie
        currentMovie = (int)Math.round(Math.random()*(movies.length-1));
        videoView =(VideoView)findViewById(R.id.videoView);
        Uri uri= Uri.fromFile(movies[currentMovie]);
        videoView.setVideoURI(uri);
        videoView.requestFocus();
        Log.d(this.getClass().toString(), "Focus");
        videoView.setOnPreparedListener(this);
        videoView.setOnErrorListener(this);
        videoView.setOnCompletionListener(this);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        videoView.stopPlayback();
        Log.d(TAG, "Detatched");
    }

}