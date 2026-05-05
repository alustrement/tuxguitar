package app.tuxguitar.converter.api;

import java.nio.file.Path;
import java.util.UUID;

public record JobResult(UUID jobId, Path inputFile, Path outputFile, boolean compressedOutput, String callbackUrl,
		JobStatus status, String errorMessage) {

	public static JobResult queued(UUID jobId, Path inputFile, Path outputFile, boolean compressedOutput, String callbackUrl) {
		return new JobResult(jobId, inputFile, outputFile, compressedOutput, callbackUrl, JobStatus.QUEUED, null);
	}

	public JobResult withStatus(JobStatus newStatus, String newErrorMessage) {
		return new JobResult(this.jobId, this.inputFile, this.outputFile, this.compressedOutput, this.callbackUrl, newStatus, newErrorMessage);
	}

	public String outputFileName() {
		return this.outputFile.getFileName().toString();
	}
}