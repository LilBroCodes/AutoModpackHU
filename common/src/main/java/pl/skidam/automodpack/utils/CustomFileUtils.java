package pl.skidam.automodpack.utils;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import pl.skidam.automodpack.AutoModpack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CustomFileUtils {
    public static void forceDelete(File file, boolean deleteOnExit) {

        if (file.exists()) {
            FileUtils.deleteQuietly(file);

            if (file.exists()) {
                try {
                    FileDeleteStrategy.FORCE.delete(file);
                } catch (IOException ignored) {
                }
            }

            if (file.exists()) { // if mod to delete still exists
                try {
                    java.io.File emptyFolder = new File(AutoModpack.automodpackDir + File.separator + "empty");
                    if (!emptyFolder.exists()) {
                        emptyFolder.mkdirs();
                    }
                    ZipTools.zipFolder(emptyFolder, file);
                    FileDeleteStrategy.FORCE.delete(emptyFolder);
                    FileDeleteStrategy.FORCE.delete(file);
                } catch (IOException ignored) {
                }
            }

            if (file.exists()) {
                try {
                    FileDeleteStrategy.FORCE.delete(file);
                } catch (IOException ignored) {
                }
            }

            if (deleteOnExit && file.exists()) {
                AutoModpack.LOGGER.info("File {} will be deleted on exit", file.getName());
                file.deleteOnExit();
            }
        }
    }

    public static void copyFile(File source, File destination) throws IOException {
        if (!destination.exists()) {
            if (!destination.getParentFile().exists()) destination.getParentFile().mkdirs();
            Files.createFile(destination.toPath());
        }
        try (FileInputStream inputStream = new FileInputStream(source);
             FileChannel sourceChannel = inputStream.getChannel();
             FileOutputStream outputStream = new FileOutputStream(destination);
             FileChannel destinationChannel = outputStream.getChannel()) {

            destinationChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }
    }

    private static final ThreadLocal<MessageDigest> DIGEST = ThreadLocal.withInitial(() -> null);

    public static String getHash(File file, String algorithm) throws Exception {
        if (!file.exists()) return null;

        MessageDigest md = DIGEST.get();
        if (md == null || !md.getAlgorithm().equals(algorithm)) {
            try {
                md = MessageDigest.getInstance(algorithm);
                DIGEST.set(md);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        assert md != null;
        md.reset();
        md.update(Files.readAllBytes(file.toPath()));

        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b & 0xff));
        }

        return sb.toString();
    }


    public static boolean compareHashWithFile(File file, String hash, String algorithm) throws Exception {
        String fileHash = getHash(file, algorithm);

        if (fileHash == null) return false;

        if (!fileHash.equals(hash)) {
            CustomFileUtils.forceDelete(file, false);
            return false;
        } else {
            return true;
        }
    }
}