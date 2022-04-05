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

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.mail.MailHeaders;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.messaging.Message;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.nio.file.Paths;

/**
 * This transformer can handle ssl, tls and attachments for
 * the mail sink.
 *
 * @author Franck Marchand
 */
public class MailTransformer {
    public static final String MAIL_ATTACHMENT = "mail_attachment";

    @Autowired
    private JavaMailSenderImpl sender;

    public Message<String> sendMail(final Message<String> msg) {

        MimeMessage mimeMsg = sender.createMimeMessage();

        String subject = (String) msg.getHeaders().get(MailHeaders.SUBJECT);
        final String validSubject = subject!=null ? subject : "";
        final String to = (String) msg.getHeaders().get(MailHeaders.TO);
        final String cc = (String) msg.getHeaders().get(MailHeaders.CC);
        final String bcc = (String) msg.getHeaders().get(MailHeaders.BCC);
        final String from = (String) msg.getHeaders().get(MailHeaders.FROM);
        final String replyTo = (String) msg.getHeaders().get(MailHeaders.REPLY_TO);
        final String attachmentFilename = (String) msg.getHeaders().get(MailHeaders.ATTACHMENT_FILENAME);
        final String attachment = (String) msg.getHeaders().get(MAIL_ATTACHMENT);

        try {
            sender.send(new MimeMessagePreparator() {
                @Override
                public void prepare(MimeMessage mimeMessage) throws Exception {
                    MimeMessageHelper mMsg = new MimeMessageHelper(mimeMessage, true);

                    mMsg.setTo(to);
                    mMsg.setFrom(from);
                    mMsg.setReplyTo(replyTo);
                    mMsg.setSubject(validSubject);

                    if (bcc != null)
                        mMsg.setBcc(bcc);
                    if (cc != null)
                        mMsg.setCc(cc);

                    mMsg.setText(msg.getPayload());

                    if (attachment != null && attachmentFilename != null) {
                        String[] attachments;
                        if(attachment.contains(";"))
                            attachments = StringUtils.split(attachment, ";");
                        else attachments = new String[] { attachment };

                        String[] attachmentFilenames;
                        if(attachmentFilename.contains(";"))
                            attachmentFilenames = StringUtils.split(attachmentFilename, ";");
                        else attachmentFilenames = new String[] { attachmentFilename };

                        for(int i=0; i<attachments.length;i++) {
                            try {
                                mMsg.addAttachment(attachmentFilenames[i], Paths.get(attachments[i]).toFile());
                            } catch (MessagingException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        } catch (MailException e) {
            e.printStackTrace();
        }

        return msg;
    }
}
