package no.ssb.dlp.pseudo.core.util;

import com.google.common.base.Stopwatch;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.time.Duration;

@Slf4j
public class RxUtil {

    private RxUtil() {}

    public static Single<FileWriteResult> writeToFile(Flowable<String> data, File targetFile) {
        return Single.create(emitter -> {
            final Stopwatch stopwatch = Stopwatch.createStarted();
            final Writer writer;
            try {
                writer = new FileWriter(targetFile);
            }
            catch (IOException e) {
                FileWriteException flowablesException = new FileWriteException("Error initializing file " + targetFile, e);
                emitter.onError(flowablesException);
                throw flowablesException;
            }

            data.subscribeOn(Schedulers.io())
              .doOnError(e -> emitter.onError(new FileWriteException("Error streaming data to file", e)))
              .doAfterTerminate(() -> writer.close())
              .subscribe(
                s -> {
                    writer.write(s);
                },
                e -> {},
                () -> {
                    emitter.onSuccess(FileWriteResult.builder()
                        .duration(stopwatch.stop().elapsed())
                        .bytesWritten(Files.size(targetFile.toPath()))
                        .file(targetFile)
                      .build()
                    );
                }
              );
        });
    }

    public static class FileWriteException extends RuntimeException {
        public FileWriteException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Data
    @Builder
    public static class FileWriteResult {
        private File file;
        private Duration duration;
        private Long bytesWritten;
    }
}
