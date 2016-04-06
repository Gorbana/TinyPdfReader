package com.github.gfx.android.tinypdfreader;


import com.github.gfx.android.tinypdfreader.databinding.FragmentLoadingBinding;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class UrlLoaderFragment extends Fragment {

    private static final String kInputUri = "input_url";

    private static final String kOutputFile = "kOutputFile";

    private static final int MAX = 10_000;

    Uri inputUri;
    File outputFile;

    FragmentLoadingBinding binding;

    ResultListener resultListener;

    public static UrlLoaderFragment newInstance(Uri inputUri, File outputFile) {
        UrlLoaderFragment fragment = new UrlLoaderFragment();
        Bundle args = new Bundle();
        args.putParcelable(kInputUri, inputUri);
        args.putSerializable(kOutputFile, outputFile);
        fragment.setArguments(args);
        return fragment;
    }


    public UrlLoaderFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        resultListener = (ResultListener) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inputUri = getArguments().getParcelable(kInputUri);
        outputFile = (File) getArguments().getSerializable(kOutputFile);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLoadingBinding.inflate(inflater, container, false);
        binding.progress.setMax(MAX);


        final OutputStream outputStream;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .addNetworkInterceptor(Progress.createInterceptor(new Progress.ProgressListener() {
                    @Override
                    public void update(long bytesRead, long contentLength, boolean done) {
                        binding.progress.setProgress((int) ((MAX * bytesRead) / contentLength));
                    }
                }))
                .build();

        client.newCall(new Request.Builder()
                .url(inputUri.toString())
                .get()
                .build())
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, final IOException e) {
                        binding.getRoot().post(new Runnable() {
                            @Override
                            public void run() {
                                resultListener.onLoadFailure(e);
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        byte[] buffer = new byte[1024];
                        try (InputStream is = response.body().byteStream()) {
                            while (true) {
                                int count = is.read(buffer);
                                if (count == 0) {
                                    try {
                                        Thread.sleep(10);
                                    } catch (InterruptedException e) {
                                        // ignore errors
                                    }
                                } else if (count > 0) {
                                    outputStream.write(buffer, 0, count);
                                } else {
                                    break;
                                }
                            }
                        }
                        outputStream.close();

                        binding.getRoot().post(new Runnable() {
                            @Override
                            public void run() {
                                resultListener.onLoadSuccess(response);
                            }
                        });
                    }
                });

        return binding.getRoot();
    }

    public interface ResultListener {
        @UiThread
        void onLoadFailure(IOException exception);

        @UiThread
        void onLoadSuccess(Response response);
    }
}
