/*
 * Copyright 2011-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.shell.hadoop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.FsShell;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.Trash;

import org.springframework.shell.core.ExecutionProcessor;
import org.springframework.shell.core.annotation.CliCommand;
import org.springframework.shell.core.annotation.CliOption;
import org.springframework.shell.event.ParseResult;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

/**
 * HDFS shell commands
 * 
 * @author Jarred Li
 * @author Glenn Renfro
 * 
 */
@Component
public class FsShellCommands extends ConfigurationAware implements ExecutionProcessor {

	private static final String PREFIX = "hadoop fs ";

	private static final String FALSE = "false";

	private static final String TRUE = "true";

	private static final String RECURSIVE = "recursive";

	private static final String RECURSION_HELP = "whether with recursion";

	private static final String PATH = "path";

	private static final String FROM = "from";

	private static final String SOURCE_FILE_NAMES = "source file names";

	private static final String DESTINATION_PATH_NAME = "destination path name";

	private FsShell shell;

	@PostConstruct
	public void init() {
		shell = new FsShell(getHadoopConfiguration());
	}

	@Override
	protected String failedComponentName() {
		return "shell";
	}

	@Override
	protected boolean configurationChanged() {
		if (shell != null) {
			LOG.info("Hadoop configuration changed, re-initializing shell...");
		}
		init();
		return true;
	}

	@Override
	public ParseResult beforeInvocation(ParseResult invocationContext) {
		ParseResult result = super.beforeInvocation(invocationContext);
		String defaultNameKey = (String) ReflectionUtils.getField(
				ReflectionUtils.findField(FileSystem.class, "FS_DEFAULT_NAME_KEY"), null);
		String fs = getHadoopConfiguration().get(defaultNameKey);
		if (fs != null && fs.length() > 0) {
			return result;
		}
		else {
			LOG.error("You must set namenode URL before running fs commands. Use `hadoop config fs` to configure namenode URL.");
			throw new IllegalStateException("You must set fs URL before running fs commands");
		}
	}

	@CliCommand(value = PREFIX + "ls", help = "List files in the directory")
	public void ls(
			@CliOption(key = { "", "dir" }, mandatory = false, unspecifiedDefaultValue = ".", help = "directory to be listed") final String path,
			@CliOption(key = { RECURSIVE }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = RECURSION_HELP) final boolean recursive) {
		if (recursive) {
			List<String> argv = new ArrayList<String>();
			argv.add("-ls");
			argv.add("-R");
			String[] fileNames = path.split(" ");
			argv.addAll(Arrays.asList(fileNames));
			run(argv.toArray(new String[0]));
		}
		else {
			runCommand("-ls", path);
		}
	}

	@CliCommand(value = PREFIX + "cat", help = "Copy source paths to stdout")
	public void cat(
			@CliOption(key = { "", PATH }, mandatory = true, unspecifiedDefaultValue = ".", help = "file name to be shown") final String path) {
		runCommand("-cat", path);
	}

	@CliCommand(value = PREFIX + "chgrp", help = "Change group association of files")
	public void chgrp(
			@CliOption(key = { RECURSIVE }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = RECURSION_HELP) final boolean recursive,
			@CliOption(key = { "group" }, mandatory = true, help = "group name") final String group,
			@CliOption(key = { "", PATH }, mandatory = true, help = "path of the file whose group will be changed") final String path) {
		List<String> argv = new ArrayList<String>();
		argv.add("-chgrp");
		if (recursive) {
			argv.add("-R");
		}
		argv.add(group);
		String[] fileNames = path.split(" ");
		argv.addAll(Arrays.asList(fileNames));
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "chown", help = "Change the owner of files")
	public void chown(
			@CliOption(key = { RECURSIVE }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = RECURSION_HELP) final boolean recursive,
			@CliOption(key = { "owner" }, mandatory = true, help = "owner name") final String owner,
			@CliOption(key = { "", PATH }, mandatory = true, help = "path of the file whose ownership will be changed") final String path) {
		List<String> argv = new ArrayList<String>();
		argv.add("-chown");
		if (recursive) {
			argv.add("-R");
		}
		argv.add(owner);
		String[] fileNames = path.split(" ");
		argv.addAll(Arrays.asList(fileNames));
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "chmod", help = "Change the permissions of files")
	public void chmod(
			@CliOption(key = { RECURSIVE }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = RECURSION_HELP) final boolean recursive,
			@CliOption(key = { "mode" }, mandatory = true, help = "permission mode") final String mode,
			@CliOption(key = { "", PATH }, mandatory = true, help = "path of the file whose permissions will be changed") final String path) {
		List<String> argv = new ArrayList<String>();
		argv.add("-chmod");
		if (recursive) {
			argv.add("-R");
		}
		argv.add(mode);
		String[] fileNames = path.split(" ");
		argv.addAll(Arrays.asList(fileNames));
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "copyFromLocal", help = "Copy single src, or multiple srcs from local file system to the destination file system. Same as put")
	public void copyFromLocal(
			@CliOption(key = { FROM }, mandatory = true, help = SOURCE_FILE_NAMES) final String source,
			@CliOption(key = { "to" }, mandatory = true, help = DESTINATION_PATH_NAME) final String dest) {
		List<String> argv = new ArrayList<String>();
		argv.add("-copyFromLocal");
		String[] fileNames = source.split(" ");
		argv.addAll(Arrays.asList(fileNames));
		argv.add(dest);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "put", help = "Copy single src, or multiple srcs from local file system to the destination file system")
	public void put(@CliOption(key = { FROM }, mandatory = true, help = SOURCE_FILE_NAMES) final String source,
			@CliOption(key = { "to" }, mandatory = true, help = DESTINATION_PATH_NAME) final String dest) {
		List<String> argv = new ArrayList<String>();
		argv.add("-put");
		String[] fileNames = source.split(" ");
		argv.addAll(Arrays.asList(fileNames));
		argv.add(dest);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "moveFromLocal", help = "Similar to put command, except that the source localsrc is deleted after it's copied")
	public void moveFromLocal(
			@CliOption(key = { FROM }, mandatory = true, help = SOURCE_FILE_NAMES) final String source,
			@CliOption(key = { "to" }, mandatory = true, help = DESTINATION_PATH_NAME) final String dest) {
		List<String> argv = new ArrayList<String>();
		argv.add("-moveFromLocal");
		String[] fileNames = source.split(" ");
		argv.addAll(Arrays.asList(fileNames));
		argv.add(dest);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "copyToLocal", help = "Copy files to the local file system. Same as get")
	public void copyToLocal(
			@CliOption(key = { FROM }, mandatory = true, help = SOURCE_FILE_NAMES) final String source,
			@CliOption(key = { "to" }, mandatory = true, help = DESTINATION_PATH_NAME) final String dest,
			@CliOption(key = { "ignoreCrc" }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = "whether ignore CRC") final boolean ignoreCrc,
			@CliOption(key = { "crc" }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = "whether copy CRC") final boolean crc) {
		List<String> argv = new ArrayList<String>();
		argv.add("-copyToLocal");
		if (ignoreCrc) {
			argv.add("-ignoreCrc");
		}
		if (crc) {
			argv.add("-crc");
		}
		argv.add(source);
		argv.add(dest);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "copyMergeToLocal", help = "Takes a source directory and a destination file as input and concatenates files in src into the destination local file")
	public void copyMergeToLocal(
			@CliOption(key = { FROM }, mandatory = true, help = SOURCE_FILE_NAMES) final String source,
			@CliOption(key = { "to" }, mandatory = true, help = DESTINATION_PATH_NAME) final String dest,
			@CliOption(key = { "endline" }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = "whether add a newline character at the end of each file") final boolean endline) {
		List<String> argv = new ArrayList<String>();
		argv.add("-getmerge");
		argv.add(source);
		argv.add(dest);
		if (endline) {
			argv.add(TRUE);
		}
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "get", help = "Copy files to the local file system")
	public void get(
			@CliOption(key = { FROM }, mandatory = true, help = SOURCE_FILE_NAMES) final String source,
			@CliOption(key = { "to" }, mandatory = true, help = DESTINATION_PATH_NAME) final String dest,
			@CliOption(key = { "ignoreCrc" }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = "whether ignore CRC") final boolean ignoreCrc,
			@CliOption(key = { "crc" }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = "whether copy CRC") final boolean crc) {
		List<String> argv = new ArrayList<String>();
		argv.add("-get");
		if (ignoreCrc) {
			argv.add("-ignoreCrc");
		}
		if (crc) {
			argv.add("-crc");
		}
		argv.add(source);
		argv.add(dest);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "count", help = "Count the number of directories, files, bytes, quota, and remaining quota")
	public void count(
			@CliOption(key = { "quota" }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = "whether with quta information") final boolean quota,
			@CliOption(key = { PATH }, mandatory = true, help = "path name") final String path) {
		List<String> argv = new ArrayList<String>();
		argv.add("-count");
		if (quota) {
			argv.add("-q");
		}
		String[] fileNames = path.split(" ");
		argv.addAll(Arrays.asList(fileNames));
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "cp", help = "Copy files from source to destination. This command allows multiple sources as well in which case the destination must be a directory")
	public void cp(@CliOption(key = { FROM }, mandatory = true, help = SOURCE_FILE_NAMES) final String source,
			@CliOption(key = { "to" }, mandatory = true, help = DESTINATION_PATH_NAME) final String dest) {
		List<String> argv = new ArrayList<String>();
		argv.add("-cp");
		String[] fileNames = source.split(" ");
		argv.addAll(Arrays.asList(fileNames));
		argv.add(dest);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "mv", help = "Move source files to destination in the HDFS")
	public void mv(@CliOption(key = { FROM }, mandatory = true, help = SOURCE_FILE_NAMES) final String source,
			@CliOption(key = { "to" }, mandatory = true, help = DESTINATION_PATH_NAME) final String dest) {
		List<String> argv = new ArrayList<String>();
		argv.add("-mv");
		String[] fileNames = source.split(" ");
		argv.addAll(Arrays.asList(fileNames));
		argv.add(dest);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "du", help = "Displays sizes of files and directories contained in the given directory or the length of a file in case its just a file")
	public void du(
			@CliOption(key = { "", "dir" }, mandatory = false, unspecifiedDefaultValue = ".", help = "directory to be listed") final String path,
			@CliOption(key = { "summary" }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = "whether with summary") final boolean summary) {
		List<String> argv = new ArrayList<String>();
		if (summary) {
			argv.add("-dus");
		}
		else {
			argv.add("-du");
		}
		argv.add(path);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "expunge", help = "Empty the trash")
	public void expunge() {
		List<String> argv = new ArrayList<String>();
		argv.add("-expunge");
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "mkdir", help = "Create a new directory")
	public void mkdir(@CliOption(key = { "", "dir" }, mandatory = true, help = "directory name") final String dir) {
		List<String> argv = new ArrayList<String>();
		argv.add("-mkdir");
		argv.add(dir);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "rm", help = "Remove files in the HDFS")
	public void rm(
			@CliOption(key = { "", PATH }, mandatory = false, unspecifiedDefaultValue = ".", help = "path to be deleted") final String path,
			@CliOption(key = { "skipTrash" }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = "whether to skip trash") final boolean skipTrash,
			@CliOption(key = { RECURSIVE }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = "whether to recurse") final boolean recursive) {
		try {
			Path file = new Path(path);
			FileSystem fs = file.getFileSystem(getHadoopConfiguration());
			for (Path p : FileUtil.stat2Paths(fs.globStatus(file), file)) {
				FileStatus status = fs.getFileStatus(p);
				if (status.isDirectory() && !recursive) {
					LOG.error("To remove directory, please use 'fs rm </path/to/dir> --recursive' instead");
					return;
				}
				if (!skipTrash) {
					Trash trash = new Trash(fs, getHadoopConfiguration());
					trash.moveToTrash(p);
				}
				fs.delete(p, recursive);
			}
		}
		catch (Exception t) {
			LOG.error("Exception: run HDFS shell failed. Message is: " + t.getMessage());
		}
		catch (Error t) {
			LOG.error("Error: run HDFS shell failed. Message is: " + t.getMessage());
		}
	}

	@CliCommand(value = PREFIX + "setrep", help = "Change the replication factor of a file")
	public void setrep(
			@CliOption(key = { PATH }, mandatory = true, help = "path name") final String path,
			@CliOption(key = { "replica" }, mandatory = true, help = SOURCE_FILE_NAMES) final int replica,
			@CliOption(key = { RECURSIVE }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = RECURSION_HELP) final boolean recursive,
			@CliOption(key = { "waiting" }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = "whether wait for the replic number is eqal to the number") final boolean waiting) {
		List<String> argv = new ArrayList<String>();
		argv.add("-setrep");
		if (recursive) {
			argv.add("-R");
		}
		if (waiting) {
			argv.add("-w");
		}
		argv.add(String.valueOf(replica));
		argv.add(path);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "tail", help = "Display last kilobyte of the file to stdout")
	public void tail(
			@CliOption(key = { "", "file" }, mandatory = true, help = "file to be tailed") final String path,
			@CliOption(key = { "follow" }, mandatory = false, specifiedDefaultValue = TRUE, unspecifiedDefaultValue = FALSE, help = "whether show content while file grow") final boolean file) {
		List<String> argv = new ArrayList<String>();
		argv.add("-tail");
		if (file) {
			argv.add("-f");
		}
		argv.add(path);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "text", help = "Take a source file and output the file in text format")
	public void text(@CliOption(key = { "", "file" }, mandatory = true, help = "file to be shown") final String path) {
		List<String> argv = new ArrayList<String>();
		argv.add("-text");
		argv.add(path);
		run(argv.toArray(new String[0]));
	}

	@CliCommand(value = PREFIX + "touchz", help = "Create a file of zero length")
	public void touchz(@CliOption(key = { "", "file" }, mandatory = true, help = "file to be touched") final String path) {
		List<String> argv = new ArrayList<String>();
		argv.add("-touchz");
		argv.add(path);
		run(argv.toArray(new String[0]));
	}

	/**
	 * @param value
	 */
	private void runCommand(String command, String value) {
		List<String> argv = new ArrayList<String>();
		argv.add(command);
		String[] fileNames = value.split(" ");
		argv.addAll(Arrays.asList(fileNames));
		run(argv.toArray(new String[0]));
	}

	private void run(String[] argv) {
		try {
			shell.run(argv);
		}
		catch (Error t) {
			LOG.error("Severe: run HDFS shell failed. Message is: " + t.getMessage());
			if (t.getCause() != null) {
				LOG.error("root cause is: " + t.getCause().toString());
			}
		}
		catch (Exception t) {
			LOG.error("Exception: run HDFS shell failed. Message is: " + t.getMessage());
			if (t.getCause() != null) {
				LOG.error("root cause is: " + t.getCause().toString());
			}
		}
	}

}
