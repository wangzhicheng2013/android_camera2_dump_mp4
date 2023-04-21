package com.example.camera2dumpmp4;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

public class AvcEncoderOnSynchronous {
    private static final String TAG = "AvcEncoderOnSynchronous";
    private int mWidth;
    private int mHeight;
    private int mFrameRate;
    private MediaCodec mMediaCodec;
    private MediaMuxer mMuxer;
    private boolean mMuxerStarted = false;
    private int mVideoTrack = -1;
    private int mFrameCount = 0;
    public AvcEncoderOnSynchronous(int width, int height, int frameRate, int bitRate, String outPath) throws IOException {
        mWidth = width;
        mHeight = height;
        mFrameRate = frameRate;
        // Configure media format
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        format.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
        format.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel51);
        // Create encoder and input surface
        mMediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        // Prepare output file
        mMuxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
    }
    public void close() {
        try {
            // Stop and release everything
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;

            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
            mMuxerStarted = false;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e.getMessage());
        } finally {
            Log.i(TAG, "AvcEncoderOnSynchronous quit now!");
        }
    }
    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / mFrameRate;
    }
    public void offerEncoder(byte[] input) {
        if (null == input) {
            Log.e(TAG, "offerEncoder input is null!");
            return;
        }
        // Begin encoding
        // Feed input to encoder
        int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
        if (inputBufferIndex < 0) {
            Log.e(TAG, "dequeueInputBuffer failed!");
            return;
        }
        ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufferIndex);
        if (null == inputBuffer) {
            Log.e(TAG, "getInputBuffer is null!");
            return;
        }
        inputBuffer.clear();
        inputBuffer.put(input);
        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, computePresentationTime(mFrameCount++), 0);
        // Get output from encoder
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
        if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            if (true == mMuxerStarted) {
                throw new IllegalStateException("format changed twice");
            }
            MediaFormat newFormat = mMediaCodec.getOutputFormat();
            mVideoTrack = mMuxer.addTrack(newFormat);
            Log.d(TAG, "add video track-->" + mVideoTrack);
            if (mVideoTrack < 0) {
                Log.e(TAG, "error video track:" + mVideoTrack);
                return;
            }
            mMuxer.start();
            mMuxerStarted = true;
            Log.d(TAG, "mMuxerStarted!");
        } else {
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = mMediaCodec.getOutputBuffer(outputBufferIndex);
                // Write encoded data to muxer
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // This is the first output buffer containing the codec configuration
                    bufferInfo.size = 0;
                }
                if ((true == mMuxerStarted) && (bufferInfo.size > 0) && (bufferInfo.presentationTimeUs > 0)) {
                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    mMuxer.writeSampleData(mVideoTrack, outputBuffer, bufferInfo);
                }
                // Release output buffer
                mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 12000);
            }
        }
    }
}
