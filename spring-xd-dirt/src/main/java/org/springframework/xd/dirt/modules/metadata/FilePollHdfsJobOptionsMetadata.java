
package org.springframework.xd.dirt.modules.metadata;

import org.springframework.xd.module.options.mixins.BatchJobDeleteFilesOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobFieldNamesOptionMixin;
import org.springframework.xd.module.options.mixins.BatchJobRestartableOptionMixin;
import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * Describes the options for the filepollhdfs job.
 * 
 * @author Luke Taylor
 * @author Ilayaperumal Gopinathan
 */
@Mixin({ BatchJobRestartableOptionMixin.class, BatchJobDeleteFilesOptionMixin.class,
	BatchJobFieldNamesOptionMixin.class })
public class FilePollHdfsJobOptionsMetadata {

	private String fileName;

	private int rollover = 1000000;

	private String directory;

	private String fileExtension = "csv";

	@ModuleOption("the filename to use in HDFS")
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@ModuleOption("the number of bytes to write before creating a new file in HDFS")
	public void setRollover(int rollover) {
		this.rollover = rollover;
	}

	@ModuleOption("the directory to write the file(s) to in HDFS")
	public void setDirectory(String directory) {
		this.directory = directory;
	}

	@ModuleOption("the file extension to use")
	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	public String getFileName() {
		return fileName;
	}

	public int getRollover() {
		return rollover;
	}

	public String getDirectory() {
		return directory;
	}

	public String getFileExtension() {
		return fileExtension;
	}
}
