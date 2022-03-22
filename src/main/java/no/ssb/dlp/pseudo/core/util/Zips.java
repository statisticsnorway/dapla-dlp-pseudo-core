package no.ssb.dlp.pseudo.core.util;

import com.github.davidmoten.rx2.Bytes;
import com.github.davidmoten.rx2.Strings;
import com.google.common.base.Stopwatch;
import io.reactivex.Flowable;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import net.lingala.zip4j.progress.ProgressMonitor;
import no.ssb.dlp.pseudo.core.file.CompressionEncryptionMethod;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static no.ssb.dlp.pseudo.core.util.FileSlayer.deleteSilently;

@UtilityClass
@Slf4j
public class Zips {

    public static ZipResult zip(Path pathToZip, File content) throws IOException {
        return zip(pathToZip, content, ZipOptions.DEFAULT);
    }

    public static ZipResult zip(Path pathToZip, File content, ZipOptions options) throws IOException {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        ZipFile zipFile = new ZipFile(pathToZip.toFile(), options.getPassword());
        zipFile.addFile(content, options.toZip4jParameters());

        return ZipResult.builder()
          .duration(stopwatch.stop().elapsed())
          .files(List.of(zipFile.getFile()))
          .totalSize(sizeOf(zipFile))
          .build();
    }

    public static ZipResult zip(Path pathToZip, Flowable<String> source, String contentFilename) {
        return zip(pathToZip, source, contentFilename, ZipOptions.DEFAULT);
    }

    public static ZipResult zip(Path pathToZip, Flowable<String> source, String contentFilename, ZipOptions options) {
        final Stopwatch stopwatch = Stopwatch.createStarted();
        ZipFile zipFile = new ZipFile(pathToZip.toFile(), options.getPassword());
        zipFile.setRunInThread(true);

        ZipParameters zipParams = options.toZip4jParameters();
        zipParams.setFileNameInZip(contentFilename);

        ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
        File tmpFile = null;

        log.trace("Zipping {}", zipFile.getFile());
        try {
            if (options.preventInterimFileStorageWhenStreaming) {
                // Warning: This will buffer the entire contents into memory, which could result in an OOMException
                // if the contents are too large
                log.trace("Buffering data stream to memory and adding to zip {}", zipFile.getFile());
                zipFile.addStream(Strings.toInputStream(source), zipParams);
            }
            else {
                tmpFile = pathToZip.resolveSibling("tmp").toFile();
                RxUtil.FileWriteResult res = RxUtil.writeToFile(source, tmpFile).blockingGet();
                log.trace("Wrote tmp data {}", res.toString());
                if (options.getSplitFileSize() != null) {
                    log.trace("Creating split zip file archive");
                    zipFile.createSplitZipFile(List.of(tmpFile), zipParams, false, options.getSplitFileSize());
                }
                else {
                    zipFile.addFile(tmpFile, zipParams);
                }
            }

            while (progressMonitor.getState() != ProgressMonitor.State.READY) {
                log.trace("{} ({} {}%)", progressMonitor.getFileName(), progressMonitor.getCurrentTask(), progressMonitor.getPercentDone());
                Thread.sleep(500);
            }
            stopwatch.stop();
            if (tmpFile != null) {
                FileSlayer.deleteSilently(tmpFile);
            }

            if (progressMonitor.getResult() == ProgressMonitor.Result.ERROR) {
                throw new ZipException("Error zipping file " + zipFile.getFile(), progressMonitor.getException());
            }
            else {
                ZipResult res = ZipResult.builder()
                  .duration(stopwatch.elapsed())
                  .files(zipFile.isSplitArchive() ? zipFile.getSplitZipFiles() : List.of(zipFile.getFile()))
                  .totalSize(sizeOf(zipFile))
                  .build();
                log.debug("Done zipping file {} ({} bytes, {} files), elapsed time {}", zipFile.getFile(), res.getTotalSize(), res.getFiles().size(), res.getDuration());
                if (zipFile.isSplitArchive()) {
                    log.trace("Zip archive parts: {}", zipFile.getSplitZipFiles());
                }
                return res;
            }

        } catch (Exception e) {
            throw new ZipException("Error zipping Flowable with content=" + contentFilename + " to file " + pathToZip, e);
        }
    }

    public static Flowable<byte[]> zip(Flowable<String> source, String contentFilename) {
        return zip(source, contentFilename, ZipOptions.DEFAULT);
    }

    public static Flowable<byte[]> zip(Flowable<String> source, String contentFilename, ZipOptions options) {
        final Path tmpZipFile;
        try {
            String tmpName = UUID.randomUUID().toString();
            tmpZipFile = Files.createTempDirectory(tmpName).resolve(tmpName + ".zip");
            ZipResult result = Zips.zip(tmpZipFile, source, contentFilename, options);
            if (result.getFiles().size() > 1) {
                throw new RuntimeException("Split zip files are not supported");
            }
            return Bytes.from(tmpZipFile.toFile())
              .doAfterTerminate(() -> {
                  String debug = Optional.ofNullable(System.getProperty("dapla.pseudo.debug")).orElse("false");
                  if (! debug.equals("true")) {
                      deleteSilently(tmpZipFile);
                  }
              });
        } catch (Exception e) {
            throw new ZipException("Error zipping Flowable with content=" + contentFilename + " to Flowable", e);
        }
    }

    public static Set<File> unzip(File zippedFile, Path destPath) throws IOException {
        return unzip(zippedFile, destPath, UnzipOptions.DEFAULT);
    }

    public static Set<File> unzip(File zippedFile, Path destPath, UnzipOptions options) throws IOException {
        new ZipFile(zippedFile, options.getPassword())
          .extractAll(destPath.toAbsolutePath().toString());
        Set<File> files;
        try (Stream<Path> stream = Files.walk(destPath)) {
            files = stream
              .sorted(Comparator.naturalOrder())
              .filter(file -> !Files.isDirectory(file))
              .map(Path::toFile)
              .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        if (options.deleteAfterUnzip) {
            FileSlayer.delete(zippedFile);
        }

        return files;
    }

    public static long sizeOf(ZipFile zipFile) throws IOException {
        if (zipFile.isSplitArchive()) {
            return zipFile.getSplitZipFiles().stream()
              .map(f -> {
                  try {
                      return Files.size(f.toPath());
                  } catch (IOException e) {
                      return 0L;
                  }
              })
              .reduce(0L, Long::sum);
        }
        else {
            return Files.size(zipFile.getFile().toPath());
        }
    }

    public static class ZipException extends RuntimeException {
        public ZipException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Data
    @Builder
    public static class UnzipOptions {
        private boolean deleteAfterUnzip = false;
        private char[] password = null;

        static final UnzipOptions DEFAULT = UnzipOptions.builder()
          .deleteAfterUnzip(false)
          .build();

        public static UnzipOptions.UnzipOptionsBuilder unzipOpts() {
            return UnzipOptions.builder();
        }
    }

    @Data
    @Builder
    public static class ZipOptions {

        /** The password for the zipped archive */
        private char[] password;

        /** The encryption method for the zipped archive */
        private CompressionEncryptionMethod encryptionMethod;

        /**
         * <p>preventInterimFileStorageWhenStreaming decides if the content of a data stream (such as a Flowable) is
         * allowed to be stored temporaríly to disk while zipping. If set to true, the entire contents of
         * the stream will be loaded into memory, which could result in an OutOfMemoryException if the data stream
         * is too large. This option is only relevant when zipping streams.</p>
         *
         * <p>The default value for this property is false, meaning that all data streams are buffered to disk before
         * being zipped. Note that any temporary files will be deleted after zipping.</p>
         */
        private boolean preventInterimFileStorageWhenStreaming;

        /**
         * <p>splitFileSize is the max amount of bytes for each part of the resulting zip archive. Specifying this
         * will cause the output to potentially be split into multiple files if the result exceeds this particular
         * limit.</p>
         *
         * <p>If you do not specify a splitFileSize, then the result will be just one file of arbitrary size,
         * possibly very large.</p>
         */
        private Long splitFileSize;

        static final ZipOptions DEFAULT = ZipOptions.builder().build();

        public static ZipOptions.ZipOptionsBuilder zipOpts() {
            return ZipOptions.builder();
        }

        ZipParameters toZip4jParameters() {
            ZipParameters params = new ZipParameters();
            params.setUnixMode(true);

            if (encryptionMethod != null) {
                params.setEncryptFiles(true);
                params.setEncryptionMethod(EncryptionMethod.valueOf(encryptionMethod.name()));
                params.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
            }

            return params;
        }
    }

    @Value
    @Builder
    public static class ZipResult {
        @NonNull
        private List<File> files;

        private Duration duration;

        private Long totalSize;
    }

}
