package app.tuxguitar.converter.api;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import app.tuxguitar.io.base.TGFileFormatException;
import app.tuxguitar.io.base.TGFileFormatManager;
import app.tuxguitar.io.base.TGFileFormatUtils;
import app.tuxguitar.io.base.TGSongReaderHandle;
import app.tuxguitar.io.base.TGSongReaderHelper;
import app.tuxguitar.io.base.TGSongStreamContext;
import app.tuxguitar.io.base.TGSongWriterHandle;
import app.tuxguitar.io.gpx.v6.GPXInputStreamPlugin;
import app.tuxguitar.io.gtp.GP1InputStreamPlugin;
import app.tuxguitar.io.gtp.GP2InputStreamPlugin;
import app.tuxguitar.io.gtp.GP3InputStreamPlugin;
import app.tuxguitar.io.gtp.GP4InputStreamPlugin;
import app.tuxguitar.io.gtp.GP5InputStreamPlugin;
import app.tuxguitar.io.midi.MidiSongReaderPlugin;
import app.tuxguitar.io.musicxml.MusicXMLSongWriter;
import app.tuxguitar.io.musicxml.MusicXMLSongWriterPlugin;
import app.tuxguitar.io.plugin.TGSongReaderPlugin;
import app.tuxguitar.io.ptb.PTInputStreamPlugin;
import app.tuxguitar.io.tef2.TESongReaderPlugin;
import app.tuxguitar.song.managers.TGSongManager;
import app.tuxguitar.song.models.TGSong;
import app.tuxguitar.util.TGContext;
import app.tuxguitar.util.plugin.TGPluginException;

public class ConverterService {

	private final TGContext context;

	public ConverterService() {
		this.context = new TGContext();
		this.registerPlugins();
	}

	public void convert(Path inputFile, Path outputFile, boolean compressedOutput) throws IOException, TGFileFormatException {
		TGSongManager manager = new TGSongManager();
		TGSong song = this.readSong(inputFile, manager);
		manager.updatePreciseStart(song);
		manager.autoCompleteSilences(song);

		Files.createDirectories(outputFile.getParent());
		if (compressedOutput) {
			Path tempMusicXml = Files.createTempFile(outputFile.getParent(), "musicxml-", ".musicxml");
			try {
				this.writeMusicXml(song, manager, tempMusicXml);
				this.writeCompressedMusicXml(tempMusicXml, outputFile);
			} finally {
				Files.deleteIfExists(tempMusicXml);
			}
		} else {
			this.writeMusicXml(song, manager, outputFile);
		}
	}

	private TGSong readSong(Path inputFile, TGSongManager manager) throws IOException, TGFileFormatException {
		TGSongReaderHandle handle = new TGSongReaderHandle();
		handle.setFactory(manager.getFactory());
		handle.setContext(new TGSongStreamContext());
		handle.getContext().setAttribute(TGSongReaderHelper.ATTRIBUTE_FORMAT_CODE, TGFileFormatUtils.getFileFormatCode(inputFile.toString()));
		try (InputStream inputStream = Files.newInputStream(inputFile)) {
			handle.setInputStream(inputStream);
			TGFileFormatManager.getInstance(this.context).read(handle);
		}
		if (handle.getSong() == null) {
			throw new TGFileFormatException("Unsupported or unreadable input file");
		}
		return handle.getSong();
	}

	private void writeMusicXml(TGSong song, TGSongManager manager, Path outputFile) throws IOException, TGFileFormatException {
		TGSongWriterHandle handle = new TGSongWriterHandle();
		handle.setSong(song);
		handle.setFactory(manager.getFactory());
		handle.setFormat(MusicXMLSongWriter.FILE_FORMAT);
		handle.setContext(new TGSongStreamContext());
		try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(outputFile))) {
			handle.setOutputStream(outputStream);
			TGFileFormatManager.getInstance(this.context).write(handle);
		}
	}

	private void writeCompressedMusicXml(Path sourceMusicXml, Path outputFile) throws IOException {
		try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(outputFile))) {
			zipOutputStream.putNextEntry(new ZipEntry("META-INF/"));
			zipOutputStream.closeEntry();

			zipOutputStream.putNextEntry(new ZipEntry("META-INF/container.xml"));
			zipOutputStream.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">"
				+ "<rootfiles><rootfile full-path=\"score.musicxml\" media-type=\"application/vnd.recordare.musicxml+xml\"/>"
				+ "</rootfiles></container>").getBytes(java.nio.charset.StandardCharsets.UTF_8));
			zipOutputStream.closeEntry();

			zipOutputStream.putNextEntry(new ZipEntry("score.musicxml"));
			Files.copy(sourceMusicXml, zipOutputStream);
			zipOutputStream.closeEntry();
		}
	}

	private void registerPlugins() {
		this.connectPlugin(new GP1InputStreamPlugin());
		this.connectPlugin(new GP2InputStreamPlugin());
		this.connectPlugin(new GP3InputStreamPlugin());
		this.connectPlugin(new GP4InputStreamPlugin());
		this.connectPlugin(new GP5InputStreamPlugin());
		this.connectPlugin(new GPXInputStreamPlugin());
		this.connectPlugin(new app.tuxguitar.io.gpx.v7.GPXInputStreamPlugin());
		this.connectPlugin(new PTInputStreamPlugin());
		this.connectPlugin(new MidiSongReaderPlugin());
		this.connectPlugin(new TESongReaderPlugin());
		this.connectPlugin(new app.tuxguitar.io.tef3.TESongReaderPlugin());
		this.connectPlugin(new app.tuxguitar.io.tg.v07.TGSongReaderPluginImpl());
		this.connectPlugin(new app.tuxguitar.io.tg.v08.TGSongReaderPluginImpl());
		this.connectPlugin(new app.tuxguitar.io.tg.v09.TGSongReaderPluginImpl());
		this.connectPlugin(new app.tuxguitar.io.tg.v10.TGSongReaderPluginImpl());
		this.connectPlugin(new app.tuxguitar.io.tg.v11.TGSongReaderPluginImpl());
		this.connectPlugin(new app.tuxguitar.io.tg.v12.TGSongReaderPluginImpl());
		this.connectPlugin(new app.tuxguitar.io.tg.v13.TGSongReaderPluginImpl());
		this.connectPlugin(new app.tuxguitar.io.tg.v15.TGSongReaderPluginImpl());
		this.connectWriterPlugin(new MusicXMLSongWriterPlugin());
	}

	private void connectPlugin(TGSongReaderPlugin plugin) {
		try {
			plugin.connect(this.context);
		} catch (TGPluginException exception) {
			throw new IllegalStateException("Failed to register input plugin " + plugin.getModuleId(), exception);
		}
	}

	private void connectWriterPlugin(MusicXMLSongWriterPlugin plugin) {
		try {
			plugin.connect(this.context);
		} catch (TGPluginException exception) {
			throw new IllegalStateException("Failed to register writer plugin " + plugin.getModuleId(), exception);
		}
	}
}