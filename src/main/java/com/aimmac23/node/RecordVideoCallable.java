package com.aimmac23.node;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import com.aimmac23.exception.MissingFrameException;
import com.aimmac23.node.jna.EncoderInterface;
import com.aimmac23.node.jna.LibVPX;
import com.sun.jna.Pointer;

public class RecordVideoCallable implements Callable<File> {
	
	private static final Logger log = Logger.getLogger(RecordVideoCallable.class.getSimpleName());
	
	private final int targetFramerate;
	// how long we should sleep before recording another frame, if everything else took zero time
	private final int targetFramerateSleepTime;
	
	
	private volatile boolean shouldStop = false;

	private ScreenshotSource screenshotSource;

	private LibVPX vpxLibrary;
	private EncoderInterface encoder;
	private File outputFile;
	
	public RecordVideoCallable(int targetFramerate, ScreenshotSource screenshotSource, LibVPX vpxLibrary, EncoderInterface encoderLibrary) {
		this.targetFramerate = targetFramerate;
		this.screenshotSource = screenshotSource;
		this.vpxLibrary = vpxLibrary;
		this.encoder = encoderLibrary;
		this.targetFramerateSleepTime = (int)((1.0 / targetFramerate) * 1000.0);
	}

	void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}

	@Override
	public File call() throws Exception {
		int frames = 0;

		if (outputFile == null) {
			outputFile = File.createTempFile("screencast", ".webm");
		}
		
		log.info("Starting new recording at " + targetFramerate + " fps with resolution " 
		+ screenshotSource.getWidth() + "x" + screenshotSource.getHeight());
		Pointer context = encoder.create_context(outputFile.getCanonicalPath());
		int result = encoder.init_encoder(context, screenshotSource.getWidth(),
						screenshotSource.getHeight(), targetFramerate);
		
		handleVPXError(result, "Failed to create VPX Context", context);
		result = encoder.init_codec(context);
		
		handleVPXError(result, "Failed to create init VPX codec", context);

		result = encoder.init_image(context);
		if(result != 0) {
			throw new IllegalStateException("Failed to allocate memory for image buffer.");
		}
				
		log.info("Started recording to file: " + outputFile.getCanonicalPath());
		
		long excessTime = 0;

		long videoStartTime = System.currentTimeMillis();
		while(!shouldStop) {
			// how long to use the next frame for - should be 1 if we're not falling behind
			int frameDuration = 1 + (int)(excessTime / targetFramerateSleepTime);
			// if excessTime > TARGET_FRAMERATE_TIME. then we've just taken that into account
			excessTime = excessTime % targetFramerateSleepTime;
			long start = System.currentTimeMillis();
			
			try {
				result = screenshotSource.applyScreenshot(context);

				if(result != 0) {
					throw new IllegalStateException("Failed to convert frame to YUV format");
				}
				result = encoder.encode_next_frame(context, frameDuration);
				
				handleVPXError(result, "Failed to encode next VPX frame", context);				
			}
			catch(MissingFrameException e) {
				log.warning("Had to drop frame - reason: " + e.getMessage());
			}
			
			long finish = System.currentTimeMillis();
			frames++;
			long timeTaken = finish - start;
			// we're keeping up
			if(timeTaken < targetFramerateSleepTime) {
				Thread.sleep(targetFramerateSleepTime - timeTaken);
			}
			else {
				// we're falling behind, take that into account for the next frame
				excessTime += timeTaken;
			}
		}
		result = encoder.encode_finish(context);
		
		handleVPXError(result, "Failed to finalize video", context);
		
		long videoEndTime = System.currentTimeMillis();
		
		float duration = ((videoEndTime - videoStartTime) / 1000f);
		if(duration > 0) {
			log.info(String.format("Finished recording - frames: %d duration: %.2f seconds targetFps: %d actualFps: %.2f",
					frames, duration, targetFramerate, frames / duration));	
		}
		else {
			log.warning("Finished recording - video was zero length!");
		}
		return outputFile;
	}
	
	public void stopRecording() {
		shouldStop = true;
	}
	
	private void handleVPXError(int errorCode, String message, Pointer context) {
		if(errorCode != 0) {
			throw new IllegalStateException(message + ": " 
					+ vpxLibrary.vpx_codec_err_to_string(errorCode)
					+ " due to: " + encoder.codec_error_detail(context));
		}
	}

}
