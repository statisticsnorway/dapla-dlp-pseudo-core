package no.ssb.dlp.pseudo.core.util;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class RxUtilTest {

    private static String randomString(int length) {
        return new Random().ints(48, 123)
          .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
          .limit(length)
          .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
          .toString();
    }

    @Test
    public void flowable_writeToFile_shouldWriteContentsToFile() throws Exception {
        String someData = randomString(1024*1000);
        Flowable<String> data = Flowable.just(someData)
          .subscribeOn(Schedulers.computation())
          .delay(10, TimeUnit.MILLISECONDS);
        Path tmpTarget = Files.createTempDirectory("test").resolve("foo");
        RxUtil.FileWriteResult fileWriteResult = RxUtil.writeToFile(data, tmpTarget.toFile()).blockingGet();
        assertThat(tmpTarget.toFile().exists()).isTrue();
        assertThat(fileWriteResult.getBytesWritten()).isGreaterThan(100000);
        assertThat(someData).isEqualTo(Files.readString(tmpTarget));

        FileSlayer.deleteSilently(tmpTarget);
        assertThat(tmpTarget.toFile().exists()).isFalse();
    }

}
