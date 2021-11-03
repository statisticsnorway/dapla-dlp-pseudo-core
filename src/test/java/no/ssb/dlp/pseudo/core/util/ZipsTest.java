package no.ssb.dlp.pseudo.core.util;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import net.lingala.zip4j.ZipFile;
import no.ssb.dlp.pseudo.core.file.CompressionEncryptionMethod;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static no.ssb.dlp.pseudo.core.util.FileSlayer.deleteSilently;
import static no.ssb.dlp.pseudo.core.util.FileUtils.readFileFromClasspath;
import static no.ssb.dlp.pseudo.core.util.Zips.ZipOptions.zipOpts;
import static org.assertj.core.api.Assertions.assertThat;

class ZipsTest {

    private static final char[] PASSWORD = "thepasswordiskensentme".toCharArray();

    private static void assertValidZip(Path p) {
        assertThat(new ZipFile(p.toFile()).isValidZipFile()).isTrue();
    }

    private static void assertInvalidZip(Path p) {
        assertThat(new ZipFile(p.toFile()).isValidZipFile()).isFalse();
    }

    private static void assertEncryptedZip(Path p) throws IOException {
        assertThat(new ZipFile(p.toFile()).isEncrypted()).isTrue();
    }

    private static void assertSplitArchiveZipWithMultipleParts(Path p) throws IOException {
        ZipFile zipFile = new ZipFile(p.toFile());
        assertThat(zipFile.isSplitArchive() && zipFile.getSplitZipFiles().size() > 1).isTrue();
    }

    private static void assertNotSplitArchiveZip(Path p) throws IOException {
        assertThat(new ZipFile(p.toFile()).isSplitArchive()).isFalse();
    }

    private static String randomString(int length) {
        return new Random().ints(48, 123)
          .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
          .limit(length)
          .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
          .toString();
    }

    @Test
    public void jsonFile_zip_shouldCreateZippedFile() throws IOException {
        Path pathToZip = Files.createTempDirectory("tst").resolve("test.zip");
        File content = readFileFromClasspath("data/somedata.json");
        assertInvalidZip(pathToZip);
        Zips.zip(pathToZip, content);

        assertValidZip(pathToZip);
        FileSlayer.deleteSilently(pathToZip);
    }

    @Test
    public void jsonFile_zip_shouldCreateZippedAndEncryptedFile() throws IOException {
        Path pathToZip = Files.createTempDirectory("tst").resolve("test.zip");
        File content = readFileFromClasspath("data/somedata.json");
        assertInvalidZip(pathToZip);
        Zips.zip(pathToZip, content, zipOpts()
          .encryptionMethod(CompressionEncryptionMethod.AES)
          .password(PASSWORD)
          .build());

        assertValidZip(pathToZip);
        assertEncryptedZip(pathToZip);
        FileSlayer.deleteSilently(pathToZip);
    }

    @Test
    public void flowable_zipFlowable_shouldCreateZippedFile() throws Exception {
        Path pathToZip = Files.createTempDirectory("test").resolve("test.zip");
        AtomicInteger calls = new AtomicInteger();
        Flowable<String> f = Flowable.range(100, 10).map(Object::toString)
          .doOnCancel(() -> calls.incrementAndGet())
          .subscribeOn(Schedulers.computation())
          .delay(10, TimeUnit.MILLISECONDS);
        Zips.zip(pathToZip, f, "something.txt");

        assertValidZip(pathToZip);
        assertNotSplitArchiveZip(pathToZip);
        FileSlayer.deleteSilently(pathToZip);
    }

    @Test
    public void flowable_zipFlowable_shouldCreateZippedAndEncryptedFile() throws Exception {
        Path pathToZip = Files.createTempDirectory("test").resolve("test.zip");
        AtomicInteger calls = new AtomicInteger();
        Flowable<String> f = Flowable.range(100, 10).map(Object::toString)
          .doOnCancel(() -> calls.incrementAndGet())
          .subscribeOn(Schedulers.computation())
          .delay(10, TimeUnit.MILLISECONDS);
        Zips.zip(pathToZip, f, "something.txt", zipOpts()
          .encryptionMethod(CompressionEncryptionMethod.AES)
          .password(PASSWORD)
          .build());

        assertValidZip(pathToZip);
        assertEncryptedZip(pathToZip);
        assertNotSplitArchiveZip(pathToZip);
        FileSlayer.deleteSilently(pathToZip);
    }

    @Test
    public void flowable_zipFlowable_shouldCreateMultipartZippedAndEncryptedFile() throws Exception {
        Path pathToZip = Files.createTempDirectory("test").resolve("test.zip");
        String someData = randomString(1024*1000);
        Flowable<String> f = Flowable.just(someData)
          .subscribeOn(Schedulers.computation())
          .delay(10, TimeUnit.MILLISECONDS);
        Zips.zip(pathToZip, f, "something.txt", zipOpts()
          .encryptionMethod(CompressionEncryptionMethod.AES)
          .splitFileSize(65536L)
          .password(PASSWORD)
          .build());

        assertValidZip(pathToZip);
        assertEncryptedZip(pathToZip);
        assertSplitArchiveZipWithMultipleParts(pathToZip);
        FileSlayer.deleteSilently(pathToZip);
    }

    @Test
    public void flowable_zipFlowable_shouldCreateZippedFlowable() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        Flowable<String> f = Flowable.range(100, 10).map(Object::toString)
          .doOnCancel(() -> calls.incrementAndGet())
          .subscribeOn(Schedulers.computation())
          .delay(10, TimeUnit.MILLISECONDS);
        Flowable<byte[]> flowableZip = Zips.zip(f, "somecontent.txt");

        final Path pathToZip = Files.createTempDirectory("test").resolve(UUID.randomUUID() + ".zip");
        pathToZip.toFile().createNewFile();
        assertInvalidZip(pathToZip);

        flowableZip.subscribe(
          (byte[] bytes) -> {
              Files.write(pathToZip, bytes, StandardOpenOption.APPEND);
          });

        assertValidZip(pathToZip);
        assertNotSplitArchiveZip(pathToZip);
        FileSlayer.deleteSilently(pathToZip);
    }

    @Test
    public void zipFileWithSingleEntry_unzip_shouldReturnUnzippedFile() throws IOException {
        File zipFile = readFileFromClasspath("data/single-json-file.zip");
        Path destPath = Files.createTempDirectory("temp");
        Set<File> files = Zips.unzip(zipFile, destPath);

        assertThat(files.size()).isEqualTo(1);
        FileSlayer.deleteSilently(files);
    }

    @Test
    public void zipFileWithMultipleEntries_unzip_shouldReturnUnzippedFiles() throws IOException {
        File zipFile = readFileFromClasspath("data/multiple-json-files.zip");
        Path destPath = Files.createTempDirectory("temp");
        Set<File> files = Zips.unzip(zipFile, destPath);

        assertThat(files.size()).isEqualTo(10);
        deleteSilently(files);
    }


}