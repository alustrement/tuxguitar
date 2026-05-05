package app.tuxguitar.converter.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JobService {

	private final ConverterService converterService;
	private final Path jobsRoot;
	private final ExecutorService executor;
	private final Map<UUID, JobResult> jobs;
	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;

	public JobService(ConverterService converterService, Path jobsRoot) {
		this.converterService = converterService;
		this.jobsRoot = jobsRoot;
		this.executor = Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));
		this.jobs = new ConcurrentHashMap<>();
		this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
		this.objectMapper = new ObjectMapper();
	}

	public JobSubmission submit(JobRequest request) throws IOException {
		UUID jobId = UUID.randomUUID();
		Path jobDirectory = this.jobsRoot.resolve(jobId.toString());
		Files.createDirectories(jobDirectory);

		String safeInputFileName = FileNameSanitizer.sanitize(request.inputFileName());
		Path inputFile = jobDirectory.resolve("input-" + safeInputFileName);
		try (InputStream inputStream = request.inputStream()) {
			Files.copy(inputStream, inputFile, StandardCopyOption.REPLACE_EXISTING);
		}

		String outputExtension = request.compressedOutput() ? ".mxl" : ".musicxml";
		Path outputFile = jobDirectory.resolve(jobId + outputExtension);
		JobResult queuedResult = JobResult.queued(jobId, inputFile, outputFile, request.compressedOutput(), request.callbackUrl());
		this.jobs.put(jobId, queuedResult);

		this.executor.submit(() -> this.processJob(queuedResult));
		return new JobSubmission(jobId);
	}

	public JobResult findResult(UUID jobId) {
		return this.jobs.get(jobId);
	}

	public void shutdown() {
		this.executor.shutdownNow();
	}

	private void processJob(JobResult queuedResult) {
		UUID jobId = queuedResult.jobId();
		this.jobs.put(jobId, queuedResult.withStatus(JobStatus.PROCESSING, null));
		try {
			this.converterService.convert(queuedResult.inputFile(), queuedResult.outputFile(), queuedResult.compressedOutput());
			JobResult completed = queuedResult.withStatus(JobStatus.COMPLETED, null);
			this.jobs.put(jobId, completed);
			this.notifyCallback(completed);
		} catch (Exception exception) {
			this.jobs.put(jobId, queuedResult.withStatus(JobStatus.FAILED, exception.getMessage()));
		}
	}

	private void notifyCallback(JobResult result) {
		if (result.callbackUrl() == null) {
			return;
		}
		try {
			byte[] payload = this.objectMapper.writeValueAsBytes(Map.of(
				"jobId", result.jobId().toString(),
				"status", result.status().name(),
				"fileName", result.outputFileName()));
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(result.callbackUrl()))
				.timeout(Duration.ofSeconds(15))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofByteArray(payload))
				.build();
			this.httpClient.send(request, HttpResponse.BodyHandlers.discarding());
		} catch (Exception exception) {
			// Callback failures should not change conversion status.
		}
	}
}