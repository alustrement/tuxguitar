package app.tuxguitar.converter.api;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import io.javalin.Javalin;
import io.javalin.http.ContentType;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;

public final class ConverterApiApplication {

	private ConverterApiApplication() {
		super();
	}

	public static void main(String[] args) throws Exception {
		Path jobsRoot = Path.of(System.getProperty("converter.jobs.dir", "jobs"));
		Files.createDirectories(jobsRoot);

		ConverterService converterService = new ConverterService();
		JobService jobService = new JobService(converterService, jobsRoot);
		int port = Integer.parseInt(System.getProperty("converter.api.port", "8080"));

		Javalin app = Javalin.create(config -> {
			config.showJavalinBanner = false;
			config.jsonMapper(new JavalinJackson());
			config.http.maxRequestSize = 1_073_741_824L;
		}).start(port);

		app.get("/health", ctx -> ctx.result("ok"));

		app.post("/api/convert/request", ctx -> {
			JobRequest request = JobRequest.from(ctx);
			JobSubmission submission = jobService.submit(request);
			ctx.status(HttpStatus.ACCEPTED);
			ctx.json(new JobSubmissionResponse(submission.jobId().toString()));
		});

		app.get("/api/convert/result/{jobId}", ctx -> {
			final UUID jobId;
			try {
				jobId = UUID.fromString(ctx.pathParam("jobId"));
			} catch (IllegalArgumentException e) {
				ctx.status(HttpStatus.BAD_REQUEST);
				ctx.json(new ErrorResponse("Invalid job id"));
				return;
			}

			JobResult result = jobService.findResult(jobId);
			if (result == null) {
				ctx.status(HttpStatus.NOT_FOUND);
				ctx.json(new ErrorResponse("Unknown job id"));
				return;
			}
			if (result.status() == JobStatus.PROCESSING || result.status() == JobStatus.QUEUED) {
				ctx.status(HttpStatus.ACCEPTED);
				ctx.json(new PendingResponse(result.jobId().toString(), result.status().name()));
				return;
			}
			if (result.status() == JobStatus.FAILED) {
				ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
				ctx.json(new ErrorResponse(result.errorMessage()));
				return;
			}

			ctx.header("Content-Disposition", "attachment; filename=\"" + result.outputFileName() + "\"");
			ctx.contentType(result.compressedOutput() ? "application/vnd.recordare.musicxml" : ContentType.XML);
			ctx.header("X-Job-Id", result.jobId().toString());
			ctx.result(Files.newInputStream(result.outputFile()));
		});

		Runtime.getRuntime().addShutdownHook(new Thread(jobService::shutdown));
	}
}