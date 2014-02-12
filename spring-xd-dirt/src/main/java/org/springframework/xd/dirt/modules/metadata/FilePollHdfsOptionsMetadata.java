package org.springframework.xd.dirt.modules.metadata;

import org.springframework.xd.module.options.spi.ModuleOption;

/**
 * @author Luke Taylor
 */
public class FilePollHdfsOptionsMetadata {

	private boolean restartable;

	private boolean deleteFiles;

	private String names;

	private String fileName;

	private int rollover = 1000000;

	private String directory;

	private String fileExtension = "csv";

	@ModuleOption("whether the job should be restartable or not in case of failure")
	public void setRestartable(boolean restartable) {
		this.restartable = restartable;
	}

	@ModuleOption("whether to delete files after successful import")
	public void setDeleteFiles(boolean deleteFiles) {
		this.deleteFiles = deleteFiles;
	}

	@ModuleOption("the field names in the CSV file")
	public void setNames(String names) {
		this.names = names;
	}

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

	@ModuleOption("the file extension to use (defaults to 'csv')")
	public void setFileExtension(String fileExtension) {
		this.fileExtension = fileExtension;
	}

	public Boolean getRestartable() {
		return restartable;
	}

	public Boolean getDeleteFiles() {
		return deleteFiles;
	}

	public String getNames() {
		return names;
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
