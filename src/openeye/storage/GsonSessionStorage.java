package openeye.storage;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.base.Throwables;
import com.google.gson.Gson;

public class GsonSessionStorage<T> implements IAppendableStorage<T> {

	private static final DateFormat FORMATTER = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

	private final File archiveFile;

	private ZipOutputStream archiveStream;

	protected final Class<? extends T> cls;

	protected final Gson gson;

	private final String ext;

	private int counter;

	public GsonSessionStorage(File dir, String ext, Class<? extends T> cls, Gson gson) {
		this.ext = ext;
		this.gson = gson;
		this.cls = cls;
		final String name = generateId() + ".zip";
		archiveFile = new File(dir, name);
	}

	private static String generateId() {
		return FORMATTER.format(new Date());
	}

	@Override
	public IDataSource<T> createNew() {
		return createNew("data");
	}

	private ZipOutputStream archiveStream() throws IOException {
		if (archiveStream == null) {
			OutputStream stream = new FileOutputStream(archiveFile);
			archiveStream = new ZipOutputStream(stream);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						if (archiveStream != null) archiveStream.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			});
		}

		return archiveStream;
	}

	@Override
	public IDataSource<T> createNew(String id) {
		final String fullId = String.format("%s-%04d.%s", id, counter++, ext);
		return new GsonStreamSource<T>(fullId, cls, gson) {

			@Override
			public void delete() {
				throw new UnsupportedOperationException();
			}

			@Override
			protected InputStream createInputStream() {
				throw new UnsupportedOperationException();
			}

			@Override
			protected void afterWrite(Writer writer) throws IOException {
				writer.flush();
				final ZipOutputStream archiveStream = archiveStream();
				archiveStream.closeEntry();
				archiveStream.flush();
			}

			@Override
			protected OutputStream createOutputStream() {
				try {
					ZipOutputStream stream = archiveStream();
					stream.putNextEntry(new ZipEntry(fullId));
					return stream;
				} catch (Throwable t) {
					throw Throwables.propagate(t);
				}
			}

			@Override
			protected String description() {
				return String.format("%s in archive %s", fullId, archiveFile);
			}

			@Override
			protected boolean sourceExists() {
				return false;
			}
		};
	}
}
