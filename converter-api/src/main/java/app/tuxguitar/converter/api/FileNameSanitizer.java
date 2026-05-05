package app.tuxguitar.converter.api;

public final class FileNameSanitizer {

	private FileNameSanitizer() {
		super();
	}

	public static String sanitize(String fileName) {
		String source = (fileName == null || fileName.isBlank()) ? "input.bin" : fileName;
		String sanitized = source.replaceAll("[^A-Za-z0-9._-]", "_");
		return sanitized.isBlank() ? "input.bin" : sanitized;
	}
}