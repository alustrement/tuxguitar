package app.tuxguitar.converter.api;

import java.io.IOException;
import java.io.InputStream;

import io.javalin.http.BadRequestResponse;
import io.javalin.http.Context;
import io.javalin.http.UploadedFile;

public record JobRequest(String inputFileName, InputStream inputStream, boolean compressedOutput, String callbackUrl) {

	public static JobRequest from(Context ctx) throws IOException {
		UploadedFile uploadedFile = ctx.uploadedFile("inputFile");
		if (uploadedFile == null) {
			throw new BadRequestResponse("Missing multipart field: inputFile");
		}

		String compressedValue = ctx.formParam("compressedOutput");
		boolean compressedOutput = Boolean.parseBoolean(compressedValue);
		String callbackUrl = normalizeBlank(ctx.formParam("callbackUrl"));
		return new JobRequest(uploadedFile.filename(), uploadedFile.content(), compressedOutput, callbackUrl);
	}

	private static String normalizeBlank(String value) {
		if (value == null) {
			return null;
		}
		String trimmedValue = value.trim();
		return trimmedValue.isEmpty() ? null : trimmedValue;
	}
}