package com.github.gfx.android.tinypdfreader;

import com.github.gfx.android.tinypdfreader.databinding.FragmentPdfViewerBinding;

import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.IOException;

import hugo.weaving.DebugLog;
import uk.co.senab.photoview.PhotoViewAttacher;


public class PdfViewerFragment extends Fragment {

    private static final String kPdfFile = "pdf_file";

    private boolean reversed = false;

    private PdfRenderer pdfRenderer;

    private FragmentPdfViewerBinding binding;

    public PdfViewerFragment() {
        // Required empty public constructor
    }

    public static PdfViewerFragment newInstance(File pdfFile) {
        PdfViewerFragment fragment = new PdfViewerFragment();
        Bundle args = new Bundle();
        args.putSerializable(kPdfFile, pdfFile);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        pdfRenderer = createPdfRenderer();
        PdfPagerAdapter adapter = new PdfPagerAdapter(getContext(), pdfRenderer);
        adapter.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                handleTap(x, y);
            }
        });

        binding = FragmentPdfViewerBinding.inflate(inflater, container, false);
        binding.viewPager.setAdapter(adapter);
        binding.viewPager.setReversed(reversed);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        pdfRenderer.close();
        super.onDestroyView();
    }

    @DebugLog
    PdfRenderer createPdfRenderer() {
        File pdfFile = (File) getArguments().getSerializable(kPdfFile);
        try {
            ParcelFileDescriptor fd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
            return new PdfRenderer(fd);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @DebugLog
    boolean handleTap(float x, float y) {
        float thirdX = binding.viewPager.getWidth() / 3.0f;

        if (x < thirdX) {
            return binding.viewPager.arrowScroll(View.FOCUS_LEFT);
        } else if (x > 2 * thirdX) {
            return binding.viewPager.arrowScroll(View.FOCUS_RIGHT);
        }

        return false;
    }
}
