package org.apache.nifi.registry.bundle.extract.minificpp;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.Objects;

/**
 *
 * Description: Simple implementation of Knuth-Moore-Pratt. See http://www.inf.fh-flensburg.de/lang/algorithmen/pattern/kmpen.htm
 * Since this exists within the MiNiFi CPP package we know that we expect our zip to be at the end of this binary.
 *
 * Purpose: Locates the byte headers presented through the constructor
 *
 * Justification:
 * java.util.zip.* and hence all extensions thereof ( including JarInputStream) expect the Zip header to be at the
 * front of the stream. This is not a requirement by any specification. Unix and windows zip utilities allow
 * the header to be anywhere within the file. This stream will attempt to locate it.
 *
 * If we are uncertain the origin of the input stream we will attempt a forward to back lookup with a buffered
 * input stream. This will improve lookup speed. If we know that the input stream is a FileInputStream we can
 * reference the channel to determine the size, and split the file into segment walking back if we inevitably
 * know the header will be near the end of the file. This provides a benefit and replicates the behavior of *nix
 * unzip utilities.
 */
public class HeaderLocationInputStream extends InputStream {


    /**
     * Magic number to locate
     */
    private final byte[] magicNumbers;
    /**
     * Known position of the aforementioned magic bytes.
     */
    private long headerPosition=-1;
    /**
     * Buffered input stream.
     */
    private final BufferedInputStream baseStream;
    /**
     * current position
     */
    private int currentPosition = 0;
    /**
     * Expected length of the file
     */
    private long expectedLength = 0;

    /**
     * Base constructor
     * @param stream input search stream
     * @param input magic bytes to locate
     * @param reverseLookup determine if a reverse lookup is desired.
     * @throws IOException
     */
    public HeaderLocationInputStream(InputStream stream, final byte [] input, final boolean reverseLookup) throws IOException {
        Objects.requireNonNull(stream);
        Objects.requireNonNull(input);
        baseStream = new BufferedInputStream(stream);
        magicNumbers = new byte[ input.length];
        System.arraycopy(input, 0, this.magicNumbers, 0, input.length);
        if (input.length > 0) {
            if (reverseLookup && stream instanceof FileInputStream) {
                FileChannel channel = ((FileInputStream) stream).getChannel();
                expectedLength = channel.size();
                // split this up into 10ths.
                long interval = expectedLength / 10;


                /**
                 *  Knuth-Moore-Pratt doesn't work very well in reverse, particularly because the JavaInputStreams
                 *  don't work well in reverse ( since we're dealing with a generic InputStream ).
                 *  Splitting the file into segments and searching those segments is typically ideal.
                 *
                 *  This may mean that we're redundantly searching segments especially if the magic bytes are not
                 *  found within the end segment or cross a segment boundary.
                 *
                 *  In the case where we do not find the file in the last segment, we will attempt to skip
                 *
                 *  The unzip command locates these magic bytes within milliseconds, primarily because of how
                 *  I/O is performed.
                 */
                long loc = interval * 9;
                do {
                    try {
                        // let's ensure we reposition our buffers
                        baseStream.mark((int) interval);
                        channel.position(loc);
                        seekToMagicSequence();
                    } catch (IOException io) {

                    }
                    loc -= interval;
                } while (loc > 0 && headerPosition == -1);

                if (headerPosition == -1) {
                    throw new IOException("Could not find magic header");
                }
            } else {
                seekToMagicSequence();
            }
        }
    }

    /**
     * Seeks to the magic sequence
     * @throws IOException Exception in underlying stream.
     */
    private void seekToMagicSequence() throws IOException {
        // Adapted from https://en.wikipedia.org/wiki/Knuth%E2%80%93Morris%E2%80%93Pratt_algorithm
        final int[] pattern= new int[magicNumbers.length + 1 ];
        int i = 0;
        int j = -1;
        pattern[i] = j;
        while (i < magicNumbers.length) {
            while (j >= 0 && magicNumbers[i] != magicNumbers[j]) {
                j = pattern[j];
            }
            pattern[++i] = ++j;
        }

        long bytesConsumed = 0;
        int myByte = 0;
        while ((myByte = baseStream.read()) != -1) {
            bytesConsumed++;

            while (j >= 0 && (byte)myByte  != magicNumbers[j]) {
                j = pattern[j];
            }
            ++j;

            if (j == magicNumbers.length) {
                headerPosition = bytesConsumed - magicNumbers.length;
                return;
            }
        }
        throw new IOException("Could not find magic header");
    }


    /**
     * Since we located the header, there is no need to walk back again,
     * so return that header entry.
     * @return
     * @throws IOException
     */
    @Override
    public int read() throws IOException {
        if (currentPosition < magicNumbers.length){
            int ret = magicNumbers[currentPosition++];
            return ret;
        }
        return baseStream.read();
    }
}
