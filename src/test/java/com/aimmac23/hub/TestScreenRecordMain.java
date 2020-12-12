package com.aimmac23.hub;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Assert;
import org.junit.Test;

import com.aimmac23.node.VideoRecordController;
import com.aimmac23.node.args.SystemPropertyRecordArgs;
import com.aimmac23.node.jna.EncoderInterface;
import com.aimmac23.node.jna.JnaLibraryLoader;

public class TestScreenRecordMain {

    public static void main(String[] args) throws Exception {
       new TestScreenRecordMain().test4standardAlone();
        //System.exit(0);
    }

    @Test
    public void test4standardAlone() throws Exception, InterruptedException, IOException {
        JnaLibraryLoader.init();
        final EncoderInterface encoder = JnaLibraryLoader.getEncoder();
        final File tmp = SystemUtils.getJavaIoTmpDir();
        try (VideoRecordController controller =
            new VideoRecordController(new SystemPropertyRecordArgs(), JnaLibraryLoader.getLibVPX(), encoder)) {
            final File destVideo = new File(tmp,"demo1.webm");
            controller.startRecording(destVideo, 15);
            Thread.sleep(5000);
            File recording = controller.stopRecording();
            System.out.println("recorded video:" + recording);
            Assert.assertTrue("Recording file does not exist!", recording.exists());
            Assert.assertTrue("File is zero length!", recording.length() > 0);
            Assert.assertEquals(destVideo, recording);
        }
    }

}
