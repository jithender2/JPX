package com.proxy.utils;

/**
 * `Storages` provides utility methods for working with storage sizes,
 * including converting sizes to human-readable formats.
 */
public class Storages {

	public static final long KB = 1024;
	public static final long MB = KB * 1024;
	public static final long GB = MB * 1024;
	public static final long TB = GB * 1024;
	public static final long PB = TB * 1024;

	/**
	 * Converts a size in bytes to a human-readable string representation.
	 * The output will be in bytes, KB, MB, GB, TB, or PB, depending on the
	 * magnitude of the size.  The value is formatted to two decimal places
	 * for sizes larger than KB.
	 *
	 * @param size The size in bytes to convert.
	 * @return A human-readable string representation of the size.  Returns
	 *         the size as a string if it's negative.
	 */
	public static String toHumanReadableSize(long size) {
		if (size < 0) {
			return String.valueOf(size);
		}
		if (size < KB) {
			return size + " Bytes";
		}
		if (size < MB) {
			return String.format("%.2f KB", size / (double) KB);
		}
		if (size < GB) {
			return String.format("%.2f MB", size / (double) MB);
		}
		if (size < TB) {
			return String.format("%.2f GB", size / (double) GB);
		}
		if (size < PB) {
			return String.format("%.2f TB", size / (double) TB);
		}
		return String.format("%.2f PB", size / (double) PB);
	}
}