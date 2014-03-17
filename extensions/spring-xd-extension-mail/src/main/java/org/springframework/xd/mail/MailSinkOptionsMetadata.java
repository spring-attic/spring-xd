/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.mail;

import javax.validation.constraints.NotNull;

import org.springframework.xd.module.options.spi.Mixin;
import org.springframework.xd.module.options.spi.ModuleOption;


/**
 * Captures options for the {@code mail} sink module.
 * 
 * @author Eric Bottard
 */
@Mixin(MailServerMixin.class)
public class MailSinkOptionsMetadata {

	private String bcc = "null";

	private String cc = "null";

	private String contentType = "null";

	private String from = "null";

	private String replyTo = "null";

	private String subject = "null";

	private String to = "null";

	// @NotNull as a String, but the contents can be the String
	// "null", which is a SpEL expression in its own right.
	@NotNull
	public String getBcc() {
		return bcc;
	}

	@NotNull
	public String getCc() {
		return cc;
	}

	@NotNull
	public String getContentType() {
		return contentType;
	}

	@NotNull
	public String getFrom() {
		return from;
	}

	@NotNull
	public String getReplyTo() {
		return replyTo;
	}


	@NotNull
	public String getSubject() {
		return subject;
	}

	@NotNull
	public String getTo() {
		return to;
	}

	@ModuleOption("the recipient(s) that should receive a blind carbon copy (SpEL)")
	public void setBcc(String bcc) {
		this.bcc = bcc;
	}

	@ModuleOption("the recipient(s) that should receive a carbon copy (SpEL)")
	public void setCc(String cc) {
		this.cc = cc;
	}

	@ModuleOption("the content type to use when sending the email (SpEL)")
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@ModuleOption("the primary recipient(s) of the email (SpEL)")
	public void setFrom(String from) {
		this.from = from;
	}

	@ModuleOption("the address that will become the recipient if the original recipient decides to \"reply to\" the email (SpEL)")
	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}

	@ModuleOption("the email subject (SpEL)")
	public void setSubject(String subject) {
		this.subject = subject;
	}

	@ModuleOption("the primary recipient(s) of the email (SpEL)")
	public void setTo(String to) {
		this.to = to;
	}


}
